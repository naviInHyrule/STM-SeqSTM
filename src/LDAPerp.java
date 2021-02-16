import data.Corpus;
import data.Vocabulary;
import perplexity.TopicDistributions;
import perplexity.Perplexity;
import org.apache.commons.cli.*;


/**
 *
 * @author Lan Du
 *
 */
public class LDAPerp
{
	/**
	 * Build command line options.
	 * @return
	 */
	public static Options buildOption(){
		Options options = new Options();
		options.addOption(new Option("h", "print the help message"));
		options.addOption(new Option("root", true, "output file directory"));
		options.addOption(new Option("voc", true, "vocabulary file directory"));
		options.addOption(new Option("PhiFile", true, "Phi matrix phi"));
		options.addOption(new Option("CorpusFile", true, "corpus file"));
		options.addOption(new Option("ThetaFile", true, "theta file (in the same order as corpus file)"));
		options.addOption(new Option("R", true, "the number of particles"));
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
			formatter.printHelp("perplexity", options);
			System.exit(1);
		}
		/*
		 * Print out help message
		 */
		if (line.hasOption("h")) {
			formatter.printHelp("perplexity", options);
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
		* Get vocabulary file.
		*/
		String vocFile = null;
		if(line.hasOption("voc")){
			vocFile = line.getOptionValue("voc");
		}else{
			System.err.println("Please specify the vocabulary directory \'-voc\'");
			System.exit(1);
		}

		/*
		 * Get corpus file.
		 */
		String CorpusFile = null;
		if(line.hasOption("CorpusFile")){
			CorpusFile = line.getOptionValue("CorpusFile");
		}else{
			System.err.println("Please specify the documents directory \'-CorpusFile\'");
			System.exit(1);
		}

		double Precision = 1.0;
		if(line.hasOption("Precision"))
			Precision = Double.parseDouble(line.getOptionValue("-Precision"));
		/*
		 * Get theta file.
		 */
		String ThetaFile = null;
		if(line.hasOption("ThetaFile")){
			ThetaFile = line.getOptionValue("ThetaFile");
		}else{
			System.err.println("Please specify the Thetas directory \'-ThetaFile\'");
			System.exit(1);
		}
		/*
		 * Get the number of particles
		 */
		Integer R = 3;
		if(line.hasOption("R"))
			R = new Integer(line.getOptionValue("R"));
		/*
		 * Get Phi file.
		 */

		String PhiFile = null;;
		if(line.hasOption("PhiFile")){
			PhiFile = line.getOptionValue("PhiFile");
		}else{
			System.err.println("Please specify the Phi directory \'-PhiFile\'");
			System.exit(1);
		}

		Vocabulary voc = new Vocabulary();
		voc.readVocabulary(vocFile);

		Corpus docs = new Corpus(CorpusFile, voc);
		docs.DocumentTheta(ThetaFile);
		docs. checkTheta();

		TopicDistributions Phi = new TopicDistributions(PhiFile, false);


		Perplexity perp = new Perplexity(Phi, docs, R );
		perp.run(root);
	}
}
