package model;

import java.io.*;

import optimizer.*;
import data.*;
import states.*;
import util.Vector;
import util.MTRandom;
import util.SpecialFuns;

/**
 *
 * @author Lan Du
 *
 */
public abstract class TopicSampler implements Serializable {
	//Serialization
	private static final long serialVersionUID = 1L;
	private static boolean doPolya = true;
	//Model parameters
	private TopicsAss ass;
	protected Corpus corpus;
	protected StatsTables stables;
	protected Parameters modelParams;
	protected TopicDists topicDists;
	protected double[] topicProbs;
	//For temporary storage
	protected int tmpn, tmpt;

	/**
	 * @param corpus
	 * @param modelParams
	 */
	public TopicSampler(Corpus corpus,
						Parameters modelParams, GibbsConfigs configs) {
		this.corpus = corpus;
		this.modelParams = modelParams;
		ass = new TopicsAss(corpus);
		stables = new StatsTables(modelParams.numTopics(), modelParams.numTypes(), corpus);
		initialiseStates();
		topicDists = new TopicDists(corpus, modelParams.numTopics());
		topicProbs = new double[modelParams.numTopics()];


	}

	/**
	 * Return the sampled topic for a word.
	 *
	 * @param i documnet index
	 * @param j text passage index
	 * @param w word index
	 * @param k old topic index
	 * @return
	 */
	protected abstract int sample(final int i, final int j, final int w, int k);

	/**
	 * Return the minus model log likelihood.
	 *
	 * @return
	 */
	public abstract double logLikelihood();

	/**
	 * Initialise tabulated values.
	 */
	public abstract void initCachedValues();

	/**
	 * Run one Gibss sampling iteration through all the documents.
	 */
	public void runOneGibbsCycle() {
		int k;
		for (int i = 0; i < corpus.numDocs(); i++) {
			for (int j = 0; j < corpus.getDoc(i).numSegs(); j++) {
				Segment tp = corpus.getDoc(i).getSegment(j);
				for (int n = 0; n < tp.size(); n++) {
					k = ass.getTopic(i, j, n);
					k = sample(i, j, tp.getWord(n), k);
					if (k > -1)
						ass.setTopic(i, j, n, k);
				}
			}
		}
		if (Parameters.debug)
			stables.checkInvariance();
	}

	/**
	 * Sample a value from a double array;
	 *
	 * @param probs an double array
	 * @return
	 */
	protected int nextDiscrete(double[] probs) {
		double sum = 0.0;
		double r = MTRandom.nextDouble() * Vector.sum(probs);
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
			if (sum > r) return i;
		}
		return probs.length - 1;
	}

	/**
	 * Randomly initialise the states.
	 * All the customers eating the same dish will
	 * sit at one table initially.
	 */
	private void initialiseStates() {
		System.out.println("Randomly initialise the states");
		int k;
		Document doc;
		Segment tp;
		for (int i = 0; i < corpus.numDocs(); i++) {
			doc = corpus.getDoc(i);
			for (int j = 0; j < doc.numSegs(); j++) {
				tp = corpus.getDoc(i).getSegment(j);
				//Initialise topic assigment and corresponding counts
				for (int n = 0; n < tp.size(); n++) {
					k = MTRandom.nextInt(modelParams.numTopics());
					ass.setTopic(i, j, n, k);
					stables.adjustCust(i, j, tp.getWord(n), k, 1);
				}
				/*
				 * Initialise table counts by assuming
				 * all the customers eating the same dish sit
				 * at the same table.
				 */
				for (k = 0; k < modelParams.numTopics(); k++)
					if (stables.NIJK[i][j][k] > 0)
						stables.adjustTable(i, j, k, 1);
			}
		}
		if (Parameters.debug)
			stables.checkInvariance();
	}

	/**
	 * Compute the topics-by-words matrix, which is the
	 * learnt topic distributions.
	 */
	public void computePhis() {
		//System.out.println("computing phi");
		double val;
		for (int k = 0; k < modelParams.numTopics(); k++) {
			for (int w = 0; w < modelParams.numTypes(); w++) {
				val = (modelParams.getGamma(w) + stables.MKW[k][w])
						/ (modelParams.getGammaSum() + stables.MK[k]);
				assert val > 0 : "phis[" + k + "][" + w + "] = " + val;
				modelParams.setPhi(k, w, val);
			}
		}
	}

	/**
	 * Return the global log likelihood related
	 * to topic-by-word matrix.
	 *
	 * @return
	 */
	protected double globalLLL() {
		double logLikelihood = 0;
		if (modelParams.isPhiGiven()) {
			//System.out.println("phi is given");
			for (int k = 0; k < modelParams.numTopics(); k++) {
				logLikelihood += SpecialFuns.logGamma(modelParams.getGammaSum());
				for (int w = 0; w < modelParams.numTypes(); w++)
					logLikelihood += (stables.MKW[k][w] + modelParams.getGamma(w) - 1)
							* Math.log(modelParams.getPhi(k, w))
							- SpecialFuns.logGamma(modelParams.getGamma(w));
			}
		} else {
			//System.out.println("phi is not given");
			for (int k = 0; k < modelParams.numTopics(); k++) {
				logLikelihood += SpecialFuns.logGamma(modelParams.getGammaSum())
						- SpecialFuns.logGamma(modelParams.getGammaSum() + stables.MK[k]);
				for (int w = 0; w < modelParams.numTypes(); w++) {
					logLikelihood += SpecialFuns.logGamma(modelParams.getGamma(w) + stables.MKW[k][w])
							- SpecialFuns.logGamma(modelParams.getGamma(w));
				}
			}
		}
		return logLikelihood;
	}

	/**
	 * Optimize the concentration parameters
	 *
	 * @param doSlice boolean variable which indicates whether using
	 *                slice sampler or not.
	 * @param doOneB  if we optimise only corpus level concentration parameter.
	 */
	public void optimiseConcentration(boolean doSlice, boolean doOneB) {
		if (doSlice) {
			SliceBSampler sliceB = new SliceBSampler(stables, modelParams.geta(),
					MTRandom.generator());
			if (doOneB) {
				modelParams.setb(sliceB.sample(modelParams.getb(0)));
			} else {
				for (int i = 0; i < corpus.numDocs(); i++)
					modelParams.setb(i, sliceB.sample(modelParams.getb(i), i));
			}
		} else {
			ArmsBSampler armsB = new ArmsBSampler(stables, modelParams.geta(), 1.0, 1.0);
			if (doOneB) {
				modelParams.setb(armsB.sample(modelParams.getb(0)));
			} else
				for (int i = 0; i < corpus.numDocs(); i++)
					modelParams.setb(i, armsB.sample(modelParams.getb(i), i));
		}
	}

	/**
	 * Optimise symetric alphas
	 */
	public void optimiseSymmetricAlpha() {
		int numDocs = corpus.numDocs();
		double[][] observations = new double[numDocs][modelParams.numTopics()];
		double[] observationLengths = new double[numDocs];
		for (int i = 0; i < numDocs; i++) {
			observationLengths[i] = stables.TI[i];
			for (int k = 0; k < modelParams.numTopics(); k++)
				observations[i][k] = stables.TIK[i][k];
		}
		double newAlpha = 0;
		if (doPolya) {
			newAlpha = AGOptimizer.sym_polya_fit(observations,
					observationLengths,
					numDocs, modelParams.numTopics(),
					modelParams.getAlpha(0));
		} else {
			newAlpha = AGOptimizer.sym_polya_fit_newton(observations,
					observationLengths,
					numDocs, modelParams.numTopics(),
					modelParams.getAlpha(0));
		}
		modelParams.setAlpha(newAlpha);
		if (Parameters.verboseLevel >= 5000)
			System.out.printf("New alpha: %.6f, new alphaSum: %.6f\n",
					newAlpha, modelParams.getAlphaSum());
	}

	/**
	 * Optimise symmetric gammas
	 */
	public void optimiseSymmetricGamma() {
		double[][] observations = new double[modelParams.numTopics()][modelParams.numTypes()];
		double[] observationLengths = new double[modelParams.numTopics()];
		for (int k = 0; k < modelParams.numTopics(); k++) {
			observationLengths[k] = stables.MK[k];
			for (int w = 0; w < modelParams.numTypes(); w++)
				observations[k][w] = stables.MKW[k][w];
		}
		double newGamma = 0;
		if (doPolya) {
			newGamma = AGOptimizer.sym_polya_fit(observations,
					observationLengths,
					modelParams.numTopics(),
					modelParams.numTypes(),
					modelParams.getGamma(0));
		} else {
			newGamma = AGOptimizer.sym_polya_fit_newton(observations,
					observationLengths,
					modelParams.numTopics(),
					modelParams.numTypes(),
					modelParams.getGamma(0));
		}
		modelParams.setGamma(newGamma);
		if (Parameters.verboseLevel >= 5000)
			System.out.printf("new Gamma: %.6f, new GammaSum: %.6f\n",
					newGamma, modelParams.getGammaSum());
	}

	/**
	 * Return the training perplexity
	 *
	 * @return
	 */
	public double perp() {
		double perpVal = logProb(corpus);
		perpVal = Math.exp(-perpVal / corpus.numWords());
		if (!SpecialFuns.isnormal(perpVal) && perpVal < 0)
			throw new RuntimeException("Illegal perplexity value: " + perpVal);
		return perpVal;
	}

	/**
	 * Return perlexity of a given corpus.
	 *
	 * @param c
	 * @return
	 */
	public double logProb(Corpus c) {
		double val, logProb = 0;
		int i, j, n, k;
		topicDists.compute(stables, modelParams);

		if (!modelParams.isPhiGiven()) {
			this.computePhis();
		}
		for (i = 0; i < c.numDocs(); i++) {
			Document doc = c.getDoc(i);
			for (j = 0; j < doc.numSegs(); j++) {
				Segment tp = doc.getSegment(j);
				for (n = 0; n < tp.size(); n++) {
					val = 0;
					for (k = 0; k < modelParams.numTopics(); k++) {
						//System.out.println(k);
						val += modelParams.getPhi(k, tp.getWord(n))
								* topicDists.getPTDist(i, j, k);
					}

					logProb += Math.log(val);

				}
				//System.out.println("d"+c.getDoc(i).docName()+"Nd:"+c.getDoc(i).numSegs()+" s: "+j+" Ns:"+tp.size());
			}
		}
		return (logProb);
	}

	public void writeParameters(int s) throws IOException {
		String folder = modelParams.oFileDir();
		modelParams.writeParameters(s, folder);
	}

	public void writeStates(int s) throws IOException {
		String folder = modelParams.oFileDir();

		if (!modelParams.isPhiGiven()) {
			//System.out.println("computing phi");
			this.computePhis();
		}
		modelParams.writePhi(s, folder);
		//save topic distributions
		topicDists.compute(stables, modelParams);
		topicDists.writeTheta(s, folder);
		//topicDists.writePsi(s,folder);
		//stables.writeObject(folder+ "stables.obj");
		//ass.writeObject(folder+ "assignments.obj");
	}
}