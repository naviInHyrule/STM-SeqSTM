package states;

import java.io.*;
import java.util.*;
import data.Vocabulary;
import util.Vector;
import util.Matrix;
import util.MapUtil;

/**
 *
 * @author Lan Du
 * @since 2013-1-11 11:25
 */
public class Parameters implements Serializable {
	// Serialization
	private static final long serialVersionUID = 1L;
	private static final int CURRENT_SERIAL_VERSION = 0;
	private static final int NUMTOPWORDS = 100;
	// Debugging
	public static int verboseLevel;
	public static boolean debug = false;
	// Folder to save trained model
	private String oFileDir;
	// Dirichlet paramters
	private double[] gammas, alphas;
	private double gammaSum, alphaSum;
	// The per-topic word distributions
	private double[][] phis, thetas;
	// Discount parameter of PYP
	private double discount;
	// Concentration parameters of PYP
	private double[] strengths;
	private int numTopics, numTypes, numDocs;
	private boolean isPhiGiven = false;
	private boolean isThetaGiven = false;
	public Parameters() {

	}

	/**
	 *
	 * @param numTypes
	 * @param numTopics
	 * @param numDocs
	 * @param oFileDir
	 */
	public Parameters(int numTypes, int numTopics, int numDocs, String oFileDir) {
		this.numTypes = numTypes;
		this.numTopics = numTopics;
		this.numDocs = numDocs;
		this.oFileDir = oFileDir;
		alphas = new double[numTopics];
		gammas = new double[numTypes];
		strengths = new double[numDocs];
		phis = new double[numTopics][numTypes];
		thetas = new double[numDocs][numTopics];
	}

	/**
	 * Initialize the model parameters
	 *
	 * @param initDiscount
	 *            the initial discount parameter
	 * @param initStrength
	 *            the initial concentration parameter
	 * @param alpha
	 * @param gamma
	 *            the initial Dirichlet parameter
	 */
	public void init(double initDiscount, double initStrength, double alpha,
					 double gamma) {
		this.discount = initDiscount;
		Arrays.fill(strengths, initStrength);
		Arrays.fill(alphas, alpha);
		alphaSum = alpha * numTopics;
		Arrays.fill(gammas, gamma);
		gammaSum = gamma * numTypes;
	}

	/**
	 * @param initDiscount
	 * @param initStrength
	 * @param alphas
	 * @param gammas
	 * @param phis
	 */
	public void init(double initDiscount, double initStrength, double[] alphas,
					 double[] gammas, double[][] phis) {
		this.discount = initDiscount;
		Arrays.fill(strengths, initStrength);
		if (alphas.length != this.alphas.length)
			throw new RuntimeException("Illegal alpha parameters!!!");
		System.arraycopy(alphas, 0, this.alphas, 0, alphas.length);
		alphaSum = Vector.sum(this.alphas);
		if (gammas.length != this.gammas.length)
			throw new RuntimeException("Illegal gamma parameters!!!");
		System.arraycopy(gammas, 0, this.gammas, 0, gammas.length);
		gammaSum = Vector.sum(this.gammas);

		isPhiGiven = true;
		System.arraycopy(phis, 0, this.phis, 0, phis.length);
	}

	/**
	 *
	 * @param initDiscount
	 * @param initStrength
	 * @param alpha
	 * @param gamma
	 * @param phis
	 */
	public void init(double initDiscount, double initStrength, double alpha,
					 double gamma, double[][] phis) {
		this.discount = initDiscount;
		Arrays.fill(strengths, initStrength);
		Arrays.fill(alphas, alpha);
		alphaSum = alpha * numTopics;
		Arrays.fill(gammas, gamma);
		gammaSum = gamma * numTypes;

		isPhiGiven = true;
		System.arraycopy(phis, 0, this.phis, 0, phis.length);
	}

	public void init(double initDiscount, double initStrength, double[][] phis, double[][] thetas) {
		this.discount = initDiscount;
		Arrays.fill(strengths, initStrength);
		isPhiGiven = true;
		System.arraycopy(phis, 0, this.phis, 0, phis.length);
		isThetaGiven = true;
		System.arraycopy(thetas, 0, this.thetas, 0, phis.length);
	}

	public int numTopics() {
		return numTopics;
	}

	public int numTypes() {
		return numTypes;
	}

	public String oFileDir() {
		return oFileDir;
	}

	public double geta() {
		return discount;
	}

	/*
	 * Concentration parameters
	 */
	public double getb(int i) {
		return strengths[i];
	}

	public void setb(int i, double val) {
		strengths[i] = val;
	}

	public void setb(double val) {
		Arrays.fill(strengths, val);
	}

	public double meanb() {
		return Vector.mean(strengths);
	}

	/*
	 * Dirichlet parameter alpha
	 */
	public double getAlpha(int i) {
		return alphas[i];
	}

	public double[] getAlpha() {
		return alphas;
	}

	public void setAlpha(double val, int i) {
		alphaSum += val - alphas[i];
		alphas[i] = val;
	}

	public void setAlpha(double val) {
		Arrays.fill(alphas, val);
		alphaSum = val * numTopics;
	}

	public double getAlphaSum() {
		return alphaSum;
	}

	/*
	 * Dirichlet parameter gamma
	 */
	public double getGamma(int i) {
		return gammas[i];
	}

	public double[] getGamma() {
		return gammas;
	}

	public void setGamma(double val, int i) {
		gammaSum += val - gammas[i];
		gammas[i] = val;
	}

	public void setGamma(double val) {
		Arrays.fill(gammas, val);
		gammaSum = val * numTypes;
	}

	public double getGammaSum() {
		return gammaSum;
	}

	/*
	 * topic-by-word matrix
	 */
	public double getPhi(int i, int j) {
		return phis[i][j];
	}

	public double[][] getPhi() {
		return phis;
	}

	public void setPhi(int i, int j, double val) {
		phis[i][j] = val;
	}

	public boolean isPhiGiven() {
		return isPhiGiven;
	}
	public boolean isThetaGiven() {
		return isThetaGiven;
	}

	public double[][] getTheta() {
		return thetas;
	}


	public double[] getTheta(int i) {
		return thetas[i];
	}

	public double getTheta(int i,int k) {
		return thetas[i][k];
	}
	/**
	 * Print the top words for each topic
	 *
	 * @param fileName
	 * @param voc
	 */
	public void saveTopicByWords(String fileName, Vocabulary voc) {
		try {
			FileWriter fwriter = new FileWriter(new File(fileName));
			Map<String, Double> treeMap = new TreeMap<String, Double>();
			for (int k = 0; k < phis.length; k++) {
				for (int w = 0; w < phis[0].length; w++)
					treeMap.put(voc.getType(w), new Double(phis[k][w]));
				treeMap = MapUtil.sortByValueDecending(treeMap);
				fwriter.write("Topic-" + k + ": ");
				int count = 0;
				for (String key : treeMap.keySet()) {
					if (count == NUMTOPWORDS - 1)
						fwriter.write(String.format("%s(%.3f)\n", key,
								treeMap.get(key)));
					else
						fwriter.write(String.format("%s(%.3f) ", key,
								treeMap.get(key)));
					count++;
					if (count == NUMTOPWORDS)
						break;
				}
				fwriter.write("\n");
				fwriter.flush();
				treeMap.clear();
			}
			fwriter.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Save parameters
	 */
	/**
	 * Save parameters
	 */
	public void writePhi(int s, String folder) {
		String str;
		str = folder + "Phi_" + s + ".txt";
		Matrix.write(phis, str);
	}
	public void writeParameters(int s, String folder) {
		String str;
		str = folder + "b_"+s+".txt";
		Vector.write(strengths, str);
		str = folder + "alpha_"+s+".txt";
		Vector.write(alphas, str);
		str = folder + "gamma_"+s+".txt";
		Vector.write(gammas, str);
		// save in object format
		//str = folder + File.separator + "stm_final_modelParams.obj";
		//writeObject(str);
	}

	/*public void writeParameters(String folder) {
		String str;
		str = folder + File.separator + "stm_final_phi_Matrix.dat";
		Matrix.write(phis, str);
		str = folder + File.separator + "stm_final_concentrations.dat";
		Vector.write(strengths, str);
		str = folder + File.separator + "stm_final_alphas.dat";
		Vector.write(alphas, str);
		str = folder + File.separator + "stm_final_gammas.dat";
		Vector.write(gammas, str);
		// save in object format
		str = folder + File.separator + "stm_final_modelParams.obj";
		writeObject(str);
	}*/

	/**
	 * Save parameters
	 *
	 * @param fileName
	 */
	public void writeObject(String fileName) {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			// write serial version
			out.writeInt(CURRENT_SERIAL_VERSION);
			// write parameters
			out.writeInt(numTopics);
			out.writeInt(numTypes);
			out.writeInt(numDocs);
			out.writeDouble(alphaSum);
			out.writeDouble(gammaSum);
			out.writeDouble(discount);
			// write concetration parameters
			for (int i = 0; i < strengths.length; i++)
				out.writeDouble(strengths[i]);
			// write alphas
			for (int i = 0; i < alphas.length; i++)
				out.writeDouble(alphas[i]);
			// write gammas
			for (int i = 0; i < gammas.length; i++)
				out.writeDouble(gammas[i]);
			// write phis
			for (int i = 0; i < phis.length; i++)
				for (int j = 0; j < phis[i].length; j++)
					out.writeDouble(phis[i][j]);

			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public void readObject(String fileName) {
		try {
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream in = new ObjectInputStream(fis);
			// read serial version
			int version = in.readInt();
			// read parameters
			numTopics = in.readInt();
			numTypes = in.readInt();
			numDocs = in.readInt();
			alphaSum = in.readDouble();
			gammaSum = in.readDouble();
			discount = in.readDouble();
			// write concetration parameters
			strengths = new double[numDocs];
			for (int i = 0; i < strengths.length; i++)
				strengths[i] = in.readDouble();
			// read alphas
			alphas = new double[numTopics];
			for (int i = 0; i < alphas.length; i++)
				alphas[i] = in.readDouble();
			// read gammas
			gammas = new double[numTypes];
			for (int i = 0; i < gammas.length; i++)
				gammas[i] = in.readDouble();
			// read phis
			phis = new double[numTopics][numTypes];
			for (int i = 0; i < phis.length; i++)
				for (int j = 0; j < phis[i].length; j++)
					phis[i][j] = in.readDouble();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
