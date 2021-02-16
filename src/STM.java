import java.io.*;

import model.ModelEstimator;

import org.apache.commons.cli.*;

import data.Corpus;
import data.Vocabulary;
import states.GibbsConfigs;
import states.Parameters;
import util.*;

/**
 *
 * @author Lan Du
 *
 */
public class STM
{
	private String root;
	private GibbsConfigs configs;

	/**
	 *
	 * @param configs
	 * @param root
	 */
	public STM(GibbsConfigs configs,
			   String root)
	{
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
		//String[] exts = new String[2];
		//exts[0] = configs.trFileExt;
		//exts[1] = configs.teFileExt;
		Vocabulary voc = new Vocabulary();
		System.out.println(configs.vocFile);
		voc.readVocabulary(configs.vocFile);
		Corpus trCorpus = new Corpus(configs.corpusFile, voc);
		//Corpus teCorpus = null;
		//if(configs.teFileExt != null)
		//	teCorpus = new Corpus(configs.corpusFile, voc);

		if(Parameters.verboseLevel >= 1000){
			System.out.println("=======================================");
			configs.printConfiguration();
			System.out.println("voc size = "+voc.size());
			System.out.println("training dataset size = " + trCorpus.numDocs());
			System.out.println("Training corpus:");
			trCorpus.printCorpusStats();

			//if(teCorpus != null) {
			//	System.out.println("testing dataset size = " + teCorpus.numDocs());
			//	System.out.println("Testing corpus:");
			//	teCorpus.printCorpusStats();
			//}
			System.out.println("=======================================");
		}
		//Read topic-by-word matrix from a given file
		double[][] phis = null;
		if(configs.phiFile != null)
			 phis = readPhiMatrix(voc.size(), configs.phiFile);
		//Create the root directory if necessary
		//root = root + File.separator;
		(new File(root)).mkdirs();

		//Start Gibbs runs
		configs.seed = System.currentTimeMillis();
		MTRandom.setSeed(configs.seed);


		Parameters trParams = new Parameters(voc.size(),
				configs.numTopics,
				trCorpus.numDocs(), root);
		if(phis != null)
			trParams.init(configs.a, configs.b, configs.alpha, configs.gamma, phis);
		else
			trParams.init(configs.a, configs.b, configs.alpha, configs.gamma);

		ModelEstimator estimator = new ModelEstimator(trCorpus, null, trParams, configs, voc);
		estimator.estimate();

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
		options.addOption(new Option("i", false, "run indicator sampler"));
		options.addOption(new Option("k", true, "the number of topics"));
		options.addOption(new Option("alpha", true, "Dirichlet parameter alpha"));
		options.addOption(new Option("gamma", true, "Dirichlet parameter gamma"));
		options.addOption(new Option("a", true, "discount parameter of PYP"));
		options.addOption(new Option("b", true, "concetration parameter of PYP"));
		options.addOption(new Option("gnum", true, "the number of Gibbs runs"));
		options.addOption(new Option("debug", false, "turn on debugging"));
		options.addOption(new Option("verbose", true, "the verbose level"));
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
			formatter.printHelp("SeqLDA", options);
			System.exit(1);
		}
		/*
		 * Print out help message
		 */
		if (line.hasOption("h")) {
			formatter.printHelp("SeqLDA", options);
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
		if(line.hasOption("i"))
			configs.indicator = true;
		else
			configs.indicator = false;
		/*
		 * Get the number of topics for training and testing
		 */
		if(line.hasOption("k"))
			configs.numTopics = Integer.parseInt(line.getOptionValue("k"));

		if(line.hasOption("a"))
			configs.a = Double.parseDouble(line.getOptionValue("a"));
		if(line.hasOption("b"))
			configs.b = Double.parseDouble(line.getOptionValue("b"));

		if(line.hasOption("alpha"))
			configs.alpha = Double.parseDouble(line.getOptionValue("alpha"));
		if(line.hasOption("gamma"))
			configs.gamma = Double.parseDouble(line.getOptionValue("gamma"));
		/*
		 * the number of gibbs runs, the defualt value is one.
		 */
		int numGibbsRuns = 1;
		if(line.hasOption("gnum"))
			numGibbsRuns = Integer.parseInt(line.getOptionValue("gnum"));
		/*
		 * Run with debugging.
		 */
		if(line.hasOption("debug"))
			Parameters.debug = true;
		/*
		 * Verbose level
		 */
		if(line.hasOption("verbose"))
			Parameters.verboseLevel = Integer.parseInt(line.getOptionValue("verbose"));
		/*
		 * Start building the model
		 */
		STM stm = new STM(configs, root);
		stm.run();
	}
}
