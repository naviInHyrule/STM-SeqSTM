package model;

import data.Corpus;
import data.Vocabulary;
import states.GibbsConfigs;
import states.Parameters;

import java.io.FileWriter;
import java.io.PrintWriter;

/**
 *
 * @author Mariflor Vega
 *
 */
public class SeqModelEstimator {
	/*
	 * Variables for training
	 */
	private Vocabulary voc;
	private Corpus trCorpus;
	private TopicSampler trSampler;
	private Parameters trParams;
	private GibbsConfigs configs;
	/*
	 * Variables for testing
	 */
	private Corpus teCorpus = null;
	private TopicSampler teSampler;
	private Parameters teParams;
	private Corpus ttrCorpus = null;
	private Corpus tteCorpus = null;
	/*
	 * Variable for log files
	 */
	private FileWriter llihoodWriter, perpWriter, bFileWriter;
	private FileWriter alphaWriter, gammaWriter;

	/**
	 * Train the STM, and test STM if testing documents are given.
	 *
	 * @param trCorpus
	 *            training corpus
	 * @param trParams
	 *            training parameters
	 * @param configs
	 *            sampling configuration
	 * @param voc
	 *            vocabulary
	 */
	public SeqModelEstimator(Corpus trCorpus, Parameters trParams,
                             GibbsConfigs configs, Vocabulary voc) {
		this.voc = voc;
		this.trCorpus = trCorpus;
		this.trParams = trParams;
		this.configs = configs;
		trSampler = new SeqIndicatorSampler(trCorpus, trParams, configs);
		trSampler.initCachedValues();

	}

	/**
	 * Return the average perplexity of samples
	 */
	public void estimate() throws Exception {
		double perplexity = 0.0;
		int numSamples = 0;
		long iteStart, iteRTime;
		long startTime = System.currentTimeMillis();
		PrintWriter logProbWriter = new PrintWriter( trParams.oFileDir() + "log_prob.txt", "UTF-8");

		for (int ite = 1; ite <= configs.trMaxIte; ite++) {
			//System.out.println("ite:"+ite);
			iteStart = System.currentTimeMillis();

			if (ite > configs.optItrn && ite % configs.optLag==0)
				optHyperParams(configs, trSampler, false);

			trSampler.runOneGibbsCycle();

			//iteRTime = Math.round((System.currentTimeMillis() - iteStart) / 1000.0);
			//writeTrainingLogs(ite, iteRTime);


			/*
			 * Draw samples
			 */
			if (ite%configs.trLag == 0 | ite==configs.trMaxIte) {
				double logLikelihood = trSampler.logLikelihood();
				double trainPerp = trSampler.perp();
				logProbWriter.println(logLikelihood+","+trainPerp);
				logProbWriter.flush();
			}

			if (ite%configs.wrtLag == 0 | ite==configs.trMaxIte) {
				trSampler.writeStates(ite);
				trSampler.writeParameters(ite);
			}
		}
		//closeWriters();
		/*
		 * Compute total running time
		 */
		long seconds = Math.round((System.currentTimeMillis() - startTime) / 1000.0);
		long minutes = seconds / 60;
		seconds %= 60;
		long hours = minutes / 60;
		minutes %= 60;
		long days = hours / 24;
		hours %= 24;
		System.out.print("\nTotal running time: ");
		if (days != 0) {
			System.out.print(days);
			System.out.print(" days ");
		}
		if (hours != 0) {
			System.out.print(hours);
			System.out.print(" hours ");
		}
		if (minutes != 0) {
			System.out.print(minutes);
			System.out.print(" minutes ");
		}
		System.out.print(seconds);
		System.out.println(" seconds");


	}

	private double logProb() {
		return trSampler.logProb(trCorpus);
	}

	/**
	 * Return the perplexity
	 *
	 * @return

	private double computePerplexity(int sampleIndex) {
	double perp = 0.0;
	int nsamples = 0;
	String folder = trParams.oFileDir() + File.separator + "sample-"
	+ sampleIndex;
	(new File(folder)).mkdirs();
	teParams = new Parameters(trParams.numTypes(), trParams.numTopics(),
	ttrCorpus.numDocs(), folder);
	teParams.init(trParams.geta(), trParams.meanb(), trParams.getAlpha(),
	trParams.getGamma(), trParams.getPhi());
	if (configs.indicator)
	teSampler = new IndicatorSampler(ttrCorpus, teParams);
	else
	teSampler = new CollapseSampler(ttrCorpus, teParams);
	for (int ite = 1; ite <= configs.teMaxIte; ite++) {
	if (ite > 1)
	optHyperParams(configs, teSampler, true);
	teSampler.runOneGibbsCycle();
	if (Parameters.verboseLevel >= 10000) {
	String str = String.format("perplexity testing: ite = %4d, "
	+ "loglikelihood = %.3f, trPerplexity = %.3f\n", ite,
	teSampler.logLikelihood(), teSampler.perp());
	System.out.println(str);
	}
	if (configs.teBurnin > 0 && configs.teLag > 0) {
	if (ite > configs.teBurnin && ite % configs.teLag == 0) {
	perp += teSampler.perp(tteCorpus);
	nsamples++;
	}
	} else if (ite == configs.teMaxIte) {
	perp = teSampler.perp(tteCorpus);
	nsamples = 1;
	}
	}
	teSampler.writeTESamplingInfo(voc);
	perp /= nsamples;
	System.out.println("Draw sample number = " + sampleIndex + ", perp = "
	+ perp);
	return perp;
	}

	private void writeTrainingLogs(int ite, long iteRTime) throws Exception {
		double logLikelihood = trSampler.logLikelihood();
		llihoodWriter.write(String.format("%.3f\n", logLikelihood));
		llihoodWriter.flush();

		double trainPerp = trSampler.perp();
		perpWriter.write(String.format("%.3f\n", trainPerp));
		perpWriter.flush();

		if (configs.optB) {
			if(configs.oneB){
				bFileWriter.write(String.format("%.3f\n", trParams.getb(0)));
			} else {
				for (int i = 0; i < trCorpus.numDocs(); i++)
					bFileWriter.write(String.format("%.3f, ", trParams.getb(i)));
				bFileWriter.write("\n");
			}
			bFileWriter.flush();
		}

		if (configs.optAlpha) {
			alphaWriter.write(String.format("%.2f\n", trParams.getAlpha(0)));
			alphaWriter.flush();
		}

		if (configs.optGamma) {
			gammaWriter.write(String.format("%.2f\n", trParams.getGamma(0)));
			gammaWriter.flush();
		}

		if (Parameters.verboseLevel >= 3000)
			System.out.println(String.format(
					"Training Ite: %5d, runTime: %3d seconds, "
							+ "likelihood: %.3f, trainPerp: %.3f", ite,
					iteRTime, logLikelihood, trainPerp));
	}
	*/
	/**
	 * Initialise all the file writers.
	 *
	 * @throws Exception

	private void initWriters() throws Exception {
		llihoodWriter = new FileWriter(trParams.oFileDir() + File.separator
				+ "modelLogLikelihood.log");
		perpWriter = new FileWriter(trParams.oFileDir() + File.separator
				+ "traingPerplexity.log");
		if (configs.optB)
			bFileWriter = new FileWriter(trParams.oFileDir() + File.separator
					+ "optimisedConcentrations.log");
		if (configs.optAlpha)
			alphaWriter = new FileWriter(trParams.oFileDir() + File.separator
					+ "optimisedAlpha.log");
		if (configs.optGamma)
			gammaWriter = new FileWriter(trParams.oFileDir() + File.separator
					+ "optimisedGamma.log");
	}
	*/
	/**
	 * Close all the file writers.
	 *
	 * @throws Exception

	private void closeWriters() throws Exception {
		llihoodWriter.flush();
		llihoodWriter.close();
		perpWriter.flush();
		perpWriter.close();
		if (configs.optB) {
			bFileWriter.flush();
			bFileWriter.close();
		}
		if (configs.optAlpha) {
			alphaWriter.flush();
			alphaWriter.close();
		}
		if (configs.optGamma) {
			gammaWriter.flush();
			gammaWriter.close();
		}
	}
*/
	/**
	 * Optimise model hyperparameters, which include concentration parameters
	 * and Dirichlet parameters.
	 *
	 * @param sampler
	 *            gibbs sampler
	 * @param testing
	 */
	private void optHyperParams(GibbsConfigs configs, TopicSampler sampler,
								boolean testing) {
		if (configs.optB)
			sampler.optimiseConcentration(configs.slice, configs.oneB);
		if (!testing) {
			if (configs.optAlpha)
				sampler.optimiseSymmetricAlpha();
			if (configs.optGamma)
				sampler.optimiseSymmetricGamma();
		}
	}

	/**
	 * Split the testing corpus into two parts: c1: for learning topic
	 * distributions. c2: for computing perplexity.
	 *
	 * @param testing
	 *            testing corpus

	private void splitTestCorpus() {
	ArrayList<Document> doclist1 = new ArrayList<Document>();
	ArrayList<Document> doclist2 = new ArrayList<Document>();
	for (int i = 0; i < teCorpus.numDocs(); i++) {
	Document doc = teCorpus.getDoc(i);
	assert doc.getDocIndex() == i;
	ArrayList<TextPassage> tplist1 = new ArrayList<TextPassage>();
	ArrayList<TextPassage> tplist2 = new ArrayList<TextPassage>();
	for (int j = 0; j < doc.numTPs(); j++) {
	TextPassage tp = doc.getTP(j);
	assert tp.getTPIndex() == j;
	TIntArrayList wlist1 = new TIntArrayList();
	TIntArrayList wlist2 = new TIntArrayList();
	for (int n = 0; n < tp.size(); n++) {
	if (n % 2 == 0) {
	wlist1.add(tp.getWord(n));
	} else {
	wlist2.add(tp.getWord(n));
	}
	}
	if (Parameters.debug)
	if (wlist1.size() == 0 || wlist2.size() == 0)
	System.out.println("i = " + i + ", j = " + j
	+ ", wlist1 = " + wlist1.size() + ", wlist2 = "
	+ wlist2.size());

	tplist1.add(new TextPassage(wlist1, j, null));
	tplist2.add(new TextPassage(wlist2, j, null));
	}
	assert tplist1.size() == tplist2.size();
	doclist1.add(new Document(tplist1, null, i, doc.getDocName()));
	doclist2.add(new Document(tplist2, null, i, doc.getDocName()));
	}
	ttrCorpus = new Corpus(doclist1);
	tteCorpus = new Corpus(doclist2);
	assert doclist1.size() == doclist2.size();
	assert ttrCorpus.numDocs() == tteCorpus.numDocs();
	if (Parameters.debug && Parameters.verboseLevel >= 10000) {
	ttrCorpus.printCorpusStats();
	tteCorpus.printCorpusStats();
	}
	}
	 */
}
