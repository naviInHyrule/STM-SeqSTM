package model;

import java.io.Serializable;

import states.GibbsConfigs;
import states.Parameters;
import util.Matrix;
import util.MTRandom;
import util.SpecialFuns;
import util.StirNum;
import data.Corpus;

/**
 * 
 * @author Du Lan
 * @version 2013-3-13-v1
 */
public class IndicatorSampler extends TopicSampler implements Serializable{
	//Serialization
	private static final long serialVersionUID = 1L;
	//Class variables
	private double[] indOneProbs;
	//cached value
	private static double[][] stirRatioOne;
	private static double[][] stirRatioTwo;

	public IndicatorSampler (Corpus corpus,
							 Parameters modelParams, GibbsConfigs configs)
	{
		super(corpus, modelParams,configs);
		indOneProbs = new double[modelParams.numTopics()];
		if(Parameters.debug)
			System.out.println("\nRun indicator sampling algorithm!!!\n");
	}
	
	/**
	 * Initialise all the cached values.
	 */
	public void initCachedValues() {
		stirRatioOne = new double[StirNum.maxM()][StirNum.maxN()];
		Matrix.fill(stirRatioOne, Double.NaN);
		
		stirRatioTwo = new double[StirNum.maxM()][StirNum.maxN()];
		Matrix.fill(stirRatioTwo, Double.NaN);
		
		stirRatioOne[0][0] =  Math.exp(StirNum.logSN(1, 1) - StirNum.logSN(0, 0));
		for(int t = 1; t < stirRatioOne.length; t++) {
			for(int n = t; n < stirRatioOne[t].length; n++){
				stirRatioOne[t][n] = Math.exp(StirNum.logSN(n+1, t+1) - StirNum.logSN(n, t))
									*(t + 1.0)/(n + 1.0);
				stirRatioTwo[t][n] = Math.exp(StirNum.logSN(n+1, t) - StirNum.logSN(n, t))
									*(n - t + 1.0)/(n + 1.0);
			}
		}
	}
	
	/**
	 * Get a cached value.
	 * 
	 * @param n
	 *            the number of customers
	 * @param t
	 *            the number of tables
	 * @return
	 */
	private double getStirRatioOne(final int n, final int t){
		if(t >= stirRatioOne.length || n >= stirRatioOne[0].length){
			double[][] tmp = stirRatioOne;
			int sizeM = tmp.length;
			int sizeN = tmp[0].length;
			if(t > sizeM)
				sizeM += StirNum.EXPSIZE;
			if(n > sizeN)
				sizeN += StirNum.EXPSIZE;
			stirRatioOne = new double[sizeM][sizeN];
			Matrix.fill(stirRatioOne, Double.NaN);
			Matrix.copy(tmp, stirRatioOne);
		}
		if(stirRatioOne[t][n] == Double.NaN)
			stirRatioOne[t][n] = Math.exp(StirNum.logSN(n+1, t+1) - StirNum.logSN(n, t))
							 	*(t + 1.0)/(n + 1.0);
		return stirRatioOne[t][n];
	}
	
	/**
	 * Get a cached value.
	 * @param n
	 * @param t
	 * @return
	 */
	private double getStirRatioTwo (final int n, final int t){
		if(t >= stirRatioTwo.length || n >= stirRatioTwo[0].length){
			double[][] tmp = stirRatioTwo;
			int sizeM = tmp.length;
			int sizeN = tmp[0].length;
			if(t > sizeM)
				sizeM += StirNum.EXPSIZE;
			if(n > sizeN)
				sizeN += StirNum.EXPSIZE;
			stirRatioTwo = new double[sizeM][sizeN];
			Matrix.fill(stirRatioTwo, Double.NaN);
			Matrix.copy(tmp, stirRatioTwo);
		}
		if(stirRatioTwo[t][n] == Double.NaN)
			stirRatioTwo[t][n] = Math.exp(StirNum.logSN(n+1, t) - StirNum.logSN(n, t))
								*(n - t + 1.0)/(n + 1.0);
		return stirRatioTwo[t][n];
	}
	
	/**
	 * Jointly sample topic and table indicator, and
	 * return the sample topic.
	 * 
	 * @param i
	 * @param j
	 * @param w
	 * @param k
	 * @return
	 */
	protected int sample(final int i, final int j, final int w, int k){
		double phi;

		int indicator = 0;
		//Uniform distribution is used
		//System.out.println(i+" "+j+" "+w+" "+k+" "+stables.TIJK[i][j][k]+" "+stables.NIJK[i][j][k]);
		double indProb = (double) stables.TIJK[i][j][k]/stables.NIJK[i][j][k];

		if(indProb > MTRandom.nextDouble())
			indicator = 1;
		/*
		 * Note 1) if NIJK == 1, TIJK == 1, and indicator = 1;
		 * 		2) if TIJK > 1, NIJK > 1
		 */
		if(stables.NIJK[i][j][k] == 1 || stables.TIJK[i][j][k] > 1 || indicator == 0){
			if(indicator == 1) 
				stables.adjustTable(i, j, k, -1);
			stables.adjustCust(i, j, w, k, -1);
			for(k = 0; k < modelParams.numTopics(); k++){

				if (modelParams.isPhiGiven())
					phi = modelParams.getPhi(k, w);
				else
					phi = (modelParams.getGamma(w) + stables.MKW[k][w])
							/ (modelParams.getGammaSum() + stables.MK[k]);

				if(modelParams.isThetaGiven())
					indOneProbs[k] = probIndOne(i, j, k, phi, modelParams.getTheta());
				else
					indOneProbs[k] = probIndOne(i, j, k, phi);

				topicProbs[k] = indOneProbs[k];
				if(stables.TIJK[i][j][k] > 0)
					topicProbs[k] += probIndZero(i, j, k, phi);
			}
			k = this.nextDiscrete(topicProbs);
			indProb = indOneProbs[k] / topicProbs[k];			
			if (indProb > MTRandom.nextDouble())
				stables.adjustTable(i, j, k, 1);
			stables.adjustCust(i, j, w, k, 1);

			//System.out.println("sample k="+k);
			return k;
		}else{
			//System.out.println("sample k=-1");
			return -1;
		}
	}


	/**
	 * Return the joint probablity of increasing both NIJK and TIJK 
	 * by one.
	 * @param i document index
	 * @param j text passage index
	 * @param k topic index
	 * @return
	 *

	 */
	private double probIndOne(final int i, final int j, final int k, double val)
	{
		val *= this.getStirRatioOne(stables.NIJK[i][j][k], stables.TIJK[i][j][k])
				*this.probBase(i, k)
					*this.Pochratio(i,j);
		if(!SpecialFuns.isnormal(val) || val < 0)
			throw new RuntimeException("Illegal probability of ind = 1!!!");
		return val;
	}

	private double probIndOne(final int i, final int j,
							 final int k, double val, double[][] theta)
	{
		val *= this.getStirRatioOne(stables.NIJK[i][j][k], stables.TIJK[i][j][k])
				*theta[i][k]
				*this.Pochratio(i,j);
		if(!SpecialFuns.isnormal(val) || val < 0)
			throw new RuntimeException("Illegal probability of ind = 1!!!");
		return val;
	}

	private double probBase(final int i,  final int k)
	{
		double val = (modelParams.getAlpha(k) + stables.TIK[i][k])
				/(modelParams.getAlphaSum() + stables.TI[i]);

		if(!SpecialFuns.isnormal(val) || val < 0)
			throw new RuntimeException("Illegal probability of ind = 1!!!");
		return val;
	}

	private double Pochratio(final int i,  final int j)
	{
		double val = (modelParams.getb(i) + modelParams.geta()*stables.TIJ[i][j]) /
			(modelParams.getb(i) + stables.NIJ[i][j]);

		if(!SpecialFuns.isnormal(val) || val < 0)
			throw new RuntimeException("Illegal probability of ind = 1!!!");
		return val;
	}


	/**
	 * Return the joint probability of increaing NIJK by one
	 * and keeping TIJK unchanged.
	 * @param i document index
	 * @param j text passage index
	 * @param k topic index
	 * @param val pre-computed phi value
	 * @return
	 */
	public double probIndZero(final int i, final int j,
							   final int k, double val)
	{
		val *= this.getStirRatioTwo(stables.NIJK[i][j][k], stables.TIJK[i][j][k]) 
				/ (modelParams.getb(i) + stables.NIJ[i][j]);
		if(!SpecialFuns.isnormal(val) || val < 0)
			throw new RuntimeException("Illegal probability of ind = 0!!!");
		return val;
	}
	
	/**
	 * Return the minus log of joint posterior of the model.
	 */
	public double logLikelihood() {
		double logLikelihood = globalLLL();
		for (int i = 0; i < corpus.numDocs(); i++) {
			logLikelihood += SpecialFuns.logGamma(modelParams.getAlphaSum())
								- SpecialFuns.logGamma(modelParams.getAlphaSum() + stables.TI[i]);
			for (int k = 0; k < modelParams.numTopics(); k++)
				logLikelihood += SpecialFuns.logGamma(modelParams.getAlpha(k) + stables.TIK[i][k])
				 				 	- SpecialFuns.logGamma(modelParams.getAlpha(k));
			for (int j = 0; j < corpus.getDoc(i).numSegs(); j++) {
				logLikelihood += SpecialFuns.logPochSym(modelParams.getb(i), modelParams.geta(), 
														stables.TIJ[i][j])
									- SpecialFuns.logPochSym(modelParams.getb(i), 1.0, stables.NIJ[i][j]);
				for (int k = 0; k < modelParams.numTopics(); k++)
					logLikelihood += StirNum.logSN(stables.NIJK[i][j][k], stables.TIJK[i][j][k])
									- SpecialFuns.logChoose(stables.NIJK[i][j][k], stables.TIJK[i][j][k]);
			}
		}
		//logLikelihood /= - corpus.numWords();
		if (!SpecialFuns.isnormal(logLikelihood)) 
			throw new RuntimeException("Table indicator sampler has illegal log model likelihood.");
		return logLikelihood;
	}
}

//tmpn = stables.NIJK[i][j][k];
//tmpt = stables.TIJK[i][j][k];
//val *= Math.exp(StirNum.logSN(tmpn+1, tmpt+1) - StirNum.logSN(tmpn, tmpt))
//		*(modelParams.getAlpha(k) + stables.TIK[i][k])
//			*(modelParams.getb(i) + modelParams.geta()*stables.TIJ[i][j])
//			*(tmpt + 1.0)
//		/((modelParams.getAlphaSum() + stables.TI[i])
//		 	*(modelParams.getb(i) + stables.NIJ[i][j])
//		 	*(tmpn + 1.0));

//tmpn = stables.NIJK[i][j][k];
//tmpt = stables.TIJK[i][j][k];
//val *= Math.exp(StirNum.logSN(tmpn+1, tmpt) - StirNum.logSN(tmpn, tmpt))
//			*(tmpn + 1.0 - tmpt)
//		/((modelParams.getb(i) + stables.NIJ[i][j])
//		 	*(tmpn + 1.0));
