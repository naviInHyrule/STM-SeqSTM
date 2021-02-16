package model;

import java.io.Serializable;

import states.GibbsConfigs;
import states.Parameters;
import util.Matrix;
import util.Vector;
import util.SpecialFuns;
import util.StirNum;
import data.Corpus;

public class CollapseSampler extends TopicSampler implements Serializable {
	//Serialization
	private static final long serialVersionUID = 1L;
	private static final int WINSIZE = 5;
	private double[] tableProbs;
	private static double[][] stirlingRatio;
	
	public CollapseSampler(Corpus corpus,
						   Parameters modelParams, GibbsConfigs configs)
	{
		super(corpus, modelParams, configs);
		if(Parameters.debug)
			System.out.println("\nRun collapsed sampling algorithm!!!\n");
	}
	
	
	/**
	 * Initial stirling ratios
	 */
	public void initCachedValues(){
		stirlingRatio = new double[StirNum.maxM()][StirNum.maxN()];
		Matrix.fill(stirlingRatio, Double.NaN);
		for(int t = 1; t < stirlingRatio.length; t++) {
			for(int n = t; n < stirlingRatio[t].length; n++)
				stirlingRatio[t][n] = Math.exp(StirNum.logSN(n+1, t) - StirNum.logSN(n, t));
		}
	}
	
	/**
	 * Get stirling ratio
	 * 
	 * @param n
	 * @param t
	 * @return
	 */
	private double getStirlingRatio(int n, int t){
		if(t >= stirlingRatio.length || n >= stirlingRatio[0].length){
			double[][] tmp = stirlingRatio;
			int sizeM = tmp.length;
			int sizeN = tmp[0].length;
			if(t > sizeM)
				sizeM += StirNum.EXPSIZE;
			if(n > sizeN)
				sizeN += StirNum.EXPSIZE;
			stirlingRatio = new double[sizeM][sizeN];
			Matrix.fill(stirlingRatio, Double.NaN);
			Matrix.copy(tmp, stirlingRatio);
		}
		if(stirlingRatio[t][n] == Double.NaN)
			stirlingRatio[t][n] = Math.exp(StirNum.logSN(n+1, t) - StirNum.logSN(n, t));
		return stirlingRatio[t][n];
	}

	
	/**
	 * Sample topic for a word, and sample the corresponding
	 * table count.
	 * @return
	 */
	protected int sample(final int i, final int j, final int w, int k){
		k = sampleTopic(i, j, w, k);
		if(stables.NIJK[i][j][k] > 1)
			sampleTableCount(i, j, k);
		return k;
	}
	
	/**
	 * Return the sampled topic.
	 * @param i document index
	 * @param j text passage index
	 * @param w word index
	 * @param k topic index
	 * @return
	 */
	private int sampleTopic(final int i, final int j, final int w, int k){
		tmpn = stables.NIJK[i][j][k];
		tmpt = stables.TIJK[i][j][k];
		assert tmpt > 0 && tmpn >= tmpt;
		if(tmpn == tmpt)
			stables.adjustTable(i, j, k, -1);
		stables.adjustCust(i, j, w, k, -1);
		for(k = 0; k < modelParams.numTopics(); k++)
			topicProbs[k] = topicProb(i, j, w, k);
		k = nextDiscrete(topicProbs);		
		if(stables.NIJK[i][j][k] == 0){
			assert stables.TIJK[i][j][k] == 0;
			stables.adjustTable(i, j, k, 1);
		}
		stables.adjustCust(i, j, w, k, 1);		
		return k;
	}
	
	/**
	 * Topic probabilities. Here, we only distinguish the
	 * case: for each topic k, if the customer count is 
	 * zero, then create a table; otherwise, no table is created
	 * @param i documet index
	 * @param j text passage index
	 * @param w word index
	 * @param k topic index
	 * @return
	 */
	private double topicProb(final int i, final int j, final int w, final int k){		
		double val;
		if(modelParams.isPhiGiven())
			val = modelParams.getPhi(k, w);
		else
			val = (modelParams.getGamma(w) + stables.MKW[k][w])
					/(modelParams.getGammaSum() + stables.MK[k]);
		if(stables.NIJK[i][j][k] == 0){
			assert stables.TIJK[i][j][k] == 0;
			val *= (modelParams.getAlpha(k) + stables.TIK[i][k])
					* (modelParams.getb(i) + modelParams.geta()*stables.TIJ[i][j])
					/ (modelParams.getAlphaSum() + stables.TI[i]);
		}else{
			val *= this.getStirlingRatio(stables.NIJK[i][j][k], stables.TIJK[i][j][k]);
		}
		val /= (modelParams.getb(i) + stables.NIJ[i][j]);
		if(!SpecialFuns.isnormal(val) || val < 0)
			throw new RuntimeException("Illegal topicProb in collapse sampler!!! val = "+val);
		return val;
	}
	
	/**
	 * Sample table counts, given all the topic assignments.
	 * @param i document index
	 * @param j text passage index
	 * @param k topic index
	 */
	private void sampleTableCount(final int i, final int j, final int k){
		int m, diff;
		tmpn = stables.NIJK[i][j][k];
		tmpt = stables.TIJK[i][j][k];		
		int upper = tmpt + WINSIZE;
		if(upper > tmpn) 
			upper = tmpn;
		int lower = tmpt - WINSIZE;
		if(lower < 1)
			lower = 1;
		int winlength = upper - lower + 1;
		tableProbs = new double[winlength];
		for(m = 0; m < winlength; m++){
			diff = lower + m - tmpt;
			tableProbs[m] = logTableProb(i, j, k, diff);
		}
		//handling underflow
		double max = Vector.max(tableProbs);
		double tmpSum = 0, tmp;
		for(m = 0; m < winlength; m++) {
			tmp = tableProbs[m] - max;
			if(tmp > -20)
				tmpSum += Math.exp(tmp);
		}
		for(m = 0; m < winlength; m++) {
			tmp = tableProbs[m] - max;
			if(tmp > -20)
				tableProbs[m] =  Math.exp(tmp)/tmpSum;
			else
				tableProbs[m] = 0.0;
		}
		diff = nextDiscrete(tableProbs) + lower - tmpt;
		stables.adjustTable(i, j, k, diff);		
	}
	
	/**
	 * Return the log table probabilities.
	 * @param i document index
	 * @param j text passage index
	 * @param k topic
	 * @param change the difference between new table count and old table coute
	 * @return
	 */
	private double logTableProb(final int i, final int j, final int k, final int change){
		double val = SpecialFuns.logGamma(modelParams.getAlpha(k) + stables.TIK[i][k] + change)
					 	- SpecialFuns.logGamma(modelParams.getAlphaSum() + stables.TI[i] + change)
					 	+ SpecialFuns.logPochSym(modelParams.getb(i), modelParams.geta(), 
							 				     stables.TIJ[i][j]+change)
						+ StirNum.logSN(stables.NIJK[i][j][k], stables.TIJK[i][j][k]+change);
		if(!SpecialFuns.isnormal(val))
			throw new RuntimeException("Illegal table count prob in collapse sampler!!!");
		return val;
	}
	
	/**
	 * Model log likelihood
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
					logLikelihood += StirNum.logSN(stables.NIJK[i][j][k], stables.TIJK[i][j][k]);
			}
		}
		logLikelihood /= - corpus.numWords();
		if (!SpecialFuns.isnormal(logLikelihood)) {
			throw new RuntimeException("Collapse sampler has illegal log model likelihood!!!");
		}
		return logLikelihood;
	}
}
