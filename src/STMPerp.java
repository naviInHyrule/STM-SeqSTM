import data.Corpus;
import data.Vocabulary;
import model.PerpEstimator;
import org.apache.commons.cli.*;
import states.GibbsConfigs;
import states.Parameters;
import util.MTRandom;
import util.StirNum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author Mariflor Vega
 *
 */
public class STMPerp
{
	private String root;
	private GibbsConfigs configs;

	/**
	 *
	 * @param configs
	 * @param root
	 */
	public STMPerp(GibbsConfigs configs,String root) {
		this.root = root;
		this.configs = configs;
		/*
		 * make stirling table
		 */
		StirNum.initialize(configs.maxN, configs.maxM, configs.a);
	}

	/**
	 * Read topic-by-word matrix, which shoule have
	 * each row contains probablity vector, the elmenets
	 * of which are seperated by ", ".
	 * @param numTypes the vocabulary size.
	 * @param phiFile the file storing the matrix.
	 */
	private double[][] readPhiMatrix(int numTypes, String phiFile) throws IOException {
		double[][] phis = new double[configs.numTopics][numTypes];
		BufferedReader br = new BufferedReader(new FileReader(phiFile));
		for (int k = 0; k < configs.numTopics; k++) {
			String[] strs = br.readLine().split(",");
			assert strs.length == numTypes;
			for (int w = 0; w < numTypes; w++)
				phis[k][w] = Double.parseDouble(strs[w]);
		}
		br.close();
		return phis;
	}

	/**
	 * Run the STM model
	 * @throws Exception
	 */
	public void run() throws Exception {

		Vocabulary voc = new Vocabulary();
		voc.readVocabulary(configs.vocFile);
		Corpus teCorpus = new Corpus(configs.corpusFile, voc);
		teCorpus.DocumentTheta(configs.thetaFile);
		teCorpus.checkTheta();

		if(Parameters.verboseLevel >= 1000){
			System.out.println("=======================================");
			configs.printPerpConfiguration();
			System.out.println("voc size = "+voc.size());
			System.out.println("testing dataset size = " + teCorpus.numDocs());
			System.out.println("testing corpus:");
			teCorpus.printCorpusStats();
			System.out.println("=======================================");
		}
		//Read topic-by-word matrix from a given file
		double[][] phis = null;
		if(configs.phiFile != null)
			 phis = readPhiMatrix(voc.size(), configs.phiFile);

		double[][] thetas = new double[teCorpus.numDocs()][];
		if(configs.thetaFile != null){
			for(int i=0; i<teCorpus.numDocs(); i++){
				thetas[i]=teCorpus.getDoc(i).getDocTheta();
			}
		}

		//Create the root directory if necessary
		//root = root + File.separator;
		(new File(root)).mkdirs();

		//Start Gibbs runs
		configs.seed = System.currentTimeMillis();
		MTRandom.setSeed(configs.seed);


		Parameters trParams = new Parameters(voc.size(), configs.numTopics, teCorpus.numDocs(), root);
		trParams.init(configs.a, configs.b, phis,thetas);

		PerpEstimator perplexity = new PerpEstimator( teCorpus,  trParams, configs);
		perplexity.estimate();

	}

	/**
	 * Build command line options.
	 * @return
	 */
	public static Options buildOption(){
		Options options = new Options();
		options.addOption(new Option("h", "print the help message"));
		options.addOption(new Option("root", true, "output file directory"));
		@SuppressWarnings("static-access")
		Option config = OptionBuilder.withArgName("file")
				.hasArg()
				.withDescription("the model configuration file")
				.create("config");
		options.addOption(config);
		options.addOption(new Option("PhiFile", true, "Phi matrix phi"));
		options.addOption(new Option("ThetaFile", true, "theta file (in the same order as corpus file)"));
		options.addOption(new Option("T", true, "the number of topics"));
		options.addOption(new Option("a", true, "discount parameter of PYP"));
		options.addOption(new Option("b", true, "concetration parameter of PYP"));

		return options;
	}

	public static void main(String[] args) throws Exception
	{
		Options options = buildOption();
		CommandLineParser parser = new GnuParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine line = null;
		try{
			line = parser.parse(options, args);
		}catch( ParseException exp ) {
			System.err.println( "Unexpected exception:" + exp.getMessage() );
			formatter.printHelp("STMPerplexity", options);
			System.exit(1);
		}
		/*
		 * Print out help message
		 */
		if (line.hasOption("h")) {
			formatter.printHelp("STMPerplexity", options);
			System.exit(0);
		}
		/*
		 * Get the folder where all the outputs will be stored.
		 */
		String root = null;
		if(line.hasOption("root")){
			root = line.getOptionValue("root");
		}else{
			System.err.println("Please specify the output directory \'-root\'");
			System.exit(1);
		}
		/*
		 * Get configuration file.
		 */
		String configFile = null;
		if(line.hasOption("config"))
			configFile = line.getOptionValue("config");
		else{
			System.err.println("Please specify the configuration with \'-config\'");
			System.exit(1);
		}
		GibbsConfigs configs = new GibbsConfigs(configFile);

		/*
		 * Indicator sampler
		 */
		configs.indicator = true;

		/*
		 * Get the number of topics for testing
		 */
		if(line.hasOption("T"))
			configs.numTopics = Integer.parseInt(line.getOptionValue("T"));
		if(line.hasOption("a"))
			configs.a = Double.parseDouble(line.getOptionValue("a"));
		if(line.hasOption("b"))
			configs.b = Double.parseDouble(line.getOptionValue("b"));

		/*
		 * Get theta file.
		 */
		if(line.hasOption("ThetaFile")){
			configs.thetaFile = line.getOptionValue("ThetaFile");
		}else{
			System.err.println("Please specify the Thetas directory \'-ThetaFile\'");
			System.exit(1);
		}
		/*
		 * Get Phi file.
		 */
		if(line.hasOption("PhiFile")){
			configs.phiFile = line.getOptionValue("PhiFile");
		}else{
			System.err.println("Please specify the Phi directory \'-PhiFile\'");
			System.exit(1);
		}

		Parameters.debug = true;
		Parameters.verboseLevel = 5000;
		/*
		 * Start building the model
		 */
		STMPerp stm = new STMPerp(configs, root);
		stm.run();
	}
}
