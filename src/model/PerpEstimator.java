package model;

import data.Corpus;
import data.Document;
import data.Segment;
import data.Vocabulary;
import perplexity.Prior;
import states.GibbsConfigs;
import states.Parameters;
import util.MTRandom;

import java.io.FileWriter;
import java.io.PrintWriter;

/**
 *
 * @author Mariflor Vega
 *
 */
public class PerpEstimator {

	/*
	 * Variables for training
	 */

	private Parameters trParams;

	/*
	 * Variables for testing
	 */
	private Corpus teCorpus;
	private PerplexitySampler teSampler;

	/*
	 * Variable for log files
	 */
	private double[][] LLK;



	/**
	 * Train the STM, and test STM if testing documents are given.
	 *
	 * @param trParams
	 *            training parameters
	 * @param configs
	 *            sampling configuration
	 * @param teCorpus
	 *            testing corpus
	 */
	public PerpEstimator(Corpus teCorpus, Parameters trParams, GibbsConfigs configs) {

		this.teCorpus = teCorpus;
		this.trParams = trParams;
		teSampler = new PerplexitySampler(teCorpus, trParams, configs);
		teSampler.initCachedValues();

	}

	/**
	 * Return the average perplexity of samples
	 */
	public void estimate() throws Exception {
		double llk = 0.0;
		double perp;

		LLK=teSampler.runPerplexity(30);

		PrintWriter logProbWriter = new PrintWriter(trParams.oFileDir() + "llk.txt", "UTF-8");

		for (int d = 0; d <teCorpus.numDocs(); d++) {
			for (int s = 0; s < teCorpus.getDoc(d).numSegs(); s++) {
				llk+=LLK[d][s];
				logProbWriter.println(LLK[d][s] + "," + teCorpus.getDoc(d).getSegment(s).size());
				logProbWriter.flush();
			}
		}

		PrintWriter perpWriter = new PrintWriter(trParams.oFileDir() + "perp.txt", "UTF-8");
		perp=-1*llk/teCorpus.numWords();
		perpWriter.println(perp);
		perpWriter.flush();
		System.out.println("llk:"+llk+", perp:"+perp+", N:"+teCorpus.numWords());

	}



}
