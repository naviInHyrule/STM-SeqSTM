package states;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author Du Lan
 *
 */
public class GibbsConfigs {

	public long seed = System.currentTimeMillis();
	public int numTopics = 50;
	public double a = 0.0;
	public double b = 10.0;
	public double alpha = 0.1;
	public double gamma = 0.01;
	public boolean indicator = true;

	public String corpusFile;
	public String vocFile;
	public String statesFile;
	public String assFile;

	public int trMaxIte = 10000;
	public int trLag = 1000;
	public int wrtLag = 1000;
	//public int teMaxIte, teBurnin, teLag;
	public int maxN=5000, maxM=1000;

	public int optItrn=trMaxIte+1, optLag=0;
	public boolean optAlpha=false, optGamma =false;
	public boolean optB, slice, oneB;


	public String phiFile, seqFile,thetaFile;


	public GibbsConfigs(String configFile) {
		try {
			PropertiesConfiguration config = new PropertiesConfiguration();
			config.load(configFile);
			corpusFile = config.getString("corpus");
			vocFile = config.getString("vocabulary");

			maxN = config.getInt("stirling.maxN");
			maxM = config.getInt("stirling.maxM");

			/*
			 * Configure parameter optimization
			 */

			if (config.containsKey("optItrn"))
				optItrn = config.getInt("optItrn");
			else
				optItrn = 1;
			if (config.containsKey("optLag"))
				optLag = config.getInt("optLag");
			else
				optLag = 1;

			if (config.containsKey("opt.b")) {
				optB = config.getBoolean("opt.b");
				if (config.containsKey("opt.b.slice"))
					slice = config.getBoolean("opt.b.slice");
				else
					slice = false;
				if (config.containsKey("opt.b.corpus"))
					oneB = config.getBoolean("opt.b.corpus");
				else
					oneB = false;
			} else {
				optB = false;
				slice = false;
				oneB = false;
			}
			// Optimise Dirichlet parameter alpha
			if (config.containsKey("opt.alpha"))
				optAlpha = config.getBoolean("opt.alpha");
			else
				optAlpha = false;
			// Optimise Dirichlet parameter gamma
			if (config.containsKey("opt.gamma"))
				optGamma = config.getBoolean("opt.gamma");
			else
				optGamma = false;
			/*
			 * Configure the training Gibbs samplers
			 */
			//trInitIte = config.getInt("train.initItes");
			if (config.containsKey("train.maxItes"))
				trMaxIte = config.getInt("train.maxItes");
			if (config.containsKey("train.lag"))
				trLag = config.getInt("train.lag");
			if (config.containsKey("write.lag"))
				wrtLag = config.getInt("write.lag");

			if (config.containsKey("states"))
				statesFile = config.getString("states");
			else
				statesFile = null;

			if (config.containsKey("assignments"))
				assFile = config.getString("assignments");
			else
				assFile = null;

			if ((statesFile== null & assFile==null )| (statesFile!= null & assFile!=null )){
			}else{
				throw new IllegalArgumentException("statesFile and assignmentsFile must be null or no null");
			}

			/*
			 * Configure the testing Gibbs samplers
			 */
			/*
			if (config.containsKey("test.ext"))
				teFileExt = config.getString("test.ext");
			else
				teFileExt = null;

			if (teFileExt != null) {
				teMaxIte = config.getInt("test.maxItes");
				if (config.containsKey("test.burnIn"))
					teBurnin = config.getInt("test.burnIn");
				else
					teBurnin = 0;
				if (config.containsKey("test.lag"))
					teLag = config.getInt("test.lag");
				else
					teLag = 0;
			}*/
			// Input the per-topic word distributions
			if (config.containsKey("phiFile"))
				phiFile = config.getString("phiFile");
			else
				phiFile = null;

			if (config.containsKey("seqFile"))
				seqFile = config.getString("seqFile");
			else
				seqFile = null;

			// Input the per-document topic distributions
			if (config.containsKey("thetaFile"))
				thetaFile = config.getString("thetaFile");
			else
				thetaFile = null;

			/*
			 * Get the number of topics for testing
			 */
			if(config.containsKey("T"))
				numTopics = Integer.parseInt(config.getString("T"));
			if(config.containsKey("a"))
				a = Double.parseDouble(config.getString("a"));
			if(config.containsKey("b"))
				b = Double.parseDouble(config.getString("b"));


		} catch (ConfigurationException ce) {
			ce.printStackTrace();
		}
	}

	public void printConfiguration() {
		yap("Corpus = " + corpusFile);
		yap("Vocabulary = " + vocFile);
		yap("Seed = " + seed);
		yap("NumTopics = " + numTopics);

		yap("alpha = " + alpha + ", gamma = " + gamma);
		yap("a = " + a + ", b = " + b);

		yap("Sampling Concetration parameters: ");
		yap("\t optb = " + optB + ", slice = " + slice);
		yap("Optimising alpha: " + optAlpha);
		yap("Optimising gamma: " + optGamma);

		yap("Stirling maxN = " + maxN + ", Stirling maxM = " + maxM);
		yap("\t Total Itns = " + trMaxIte + ", Lag = " + trLag);
		yap("\t optItn = " + optItrn + ", optLag = " + optLag);

		//yap("\t MaxItes = " + teMaxIte + ", BurnIn = " + teBurnin + ", Lag = "+ teLag);

		yap("Phi matrix: " + phiFile);
		yap("Theta file: " + thetaFile);
		//yap("minDocFreq = " + minDocFreq + ", numTopFreq = " + numTopFreq);
	}

	public void printPerpConfiguration() {
		yap("Corpus = " + corpusFile);
		yap("Vocabulary = " + vocFile);
		yap("Seed = " + seed);
		yap("NumTopics = " + numTopics);
		yap("a = " + a + ", b = " + b);
		yap("Phi matrix: " + phiFile);
		yap("Theta file: " + thetaFile);
		//yap("minDocFreq = " + minDocFreq + ", numTopFreq = " + numTopFreq);
	}
	private void yap(Object obj) {
		System.out.println(obj);
	}
}
