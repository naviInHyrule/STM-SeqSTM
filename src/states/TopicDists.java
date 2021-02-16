package states;

import java.io.*;
import java.util.Arrays;

import data.Corpus;

/**
 * 
 * @author Lan Du, Mariflor Vega
 * @since 2013-1-11 11:25
 */
public class TopicDists implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static final int CURRENT_SERIAL_VERSION = 0;
	private final Corpus corpus;
	private double[][] docTopicDists;
	private double[][][] passageTopicDists;
	
	/**
	 * 
	 * @param c corpus
	 * @param numTopics the total number of topics
	 */
	public TopicDists(Corpus c, int numTopics)
	{
		corpus = c;
		docTopicDists = new double[c.numDocs()][numTopics];
		passageTopicDists = new double[c.numDocs()][][];
		for(int i = 0; i < c.numDocs(); i++)
			passageTopicDists[i] = new double[c.getDoc(i).numSegs()][numTopics];
	}
	
	/**
	 * topic distributions with point estimate.
	 * 
	 * @param stables
	 * @param modelParams
	 */
	public void compute(StatsTables stables, Parameters modelParams)
	{
		if(stables.TI.length != corpus.numDocs())
			throw new RuntimeException("Illegal StatsTables!!!");
		
		for(int i = 0; i < stables.TI.length; i++)
		{//for each document
			for(int k = 0; k < modelParams.numTopics(); k++)
			{//for each topic
				docTopicDists[i][k] = (modelParams.getAlpha(k) + stables.TIK[i][k]) 
										/ (modelParams.getAlphaSum() + stables.TI[i]);
				for(int j = 0; j < stables.TIJ[i].length; j++)
				{//for each text passage
					passageTopicDists[i][j][k] = ((stables.NIJK[i][j][k] - modelParams.geta()
														*stables.TIJK[i][j][k])
													+ (modelParams.geta()*stables.TIJ[i][j] 
															+ modelParams.getb(i))
														* docTopicDists[i][k]
												 )/(modelParams.getb(i) + stables.NIJ[i][j]);
				}
			}
		}
	}

	public void compute(StatsTables stables, Parameters modelParams, double[][] docTopicDists)
	{
		if(stables.TI.length != corpus.numDocs())
			throw new RuntimeException("Illegal StatsTables!!!");

		for(int i = 0; i < stables.TI.length; i++)
		{//for each document
			for(int k = 0; k < modelParams.numTopics(); k++)
			{//for each topic
				for(int j = 0; j < stables.TIJ[i].length; j++)
				{//for each text passage
					passageTopicDists[i][j][k] = ((stables.NIJK[i][j][k] - modelParams.geta()
							*stables.TIJK[i][j][k])
							+ (modelParams.geta()*stables.TIJ[i][j]
							+ modelParams.getb(i))
							* docTopicDists[i][k]
					)/(modelParams.getb(i) + stables.NIJ[i][j]);
				}
			}
		}
	}

	/**
	 * Get the k-th topic probability for the i-th document 
	 * @param i document index
	 * @param k topic index
	 * @return
	 */
	public double getDTDist(int i, int k){
		return docTopicDists[i][k];
	}
	
	/**
	 * Get a copy of the topic distribution for the i-th document
	 * @param i documen index
	 * @return
	 */
	public double[] getDTDist(int i){
		return Arrays.copyOf(docTopicDists[i], docTopicDists[i].length);
	}
	/**
	 * Get the k-th topic probability for the j-th text 
	 * passage of the i-th document
	 * @param i document index
	 * @param j text passage index
	 * @param k topic index
	 * @return
	 */
	public double getPTDist(int i, int j, int k){
		return passageTopicDists[i][j][k];
	}
	/**
	 * Get a copy of the topic distribution of the j-th text 
	 * passage of the i-th document
	 * @param i document index
	 * @param j text passage index
	 * @return
	 */
	public double[] getPTDist(int i, int j){
		return Arrays.copyOf(passageTopicDists[i][j], passageTopicDists[i][j].length);
	}
	
	/**
	 * Save topic distributions in ".dat" format
	 * @param oFileDir the output file directory
	 */
	public void writeTheta(int s, String oFileDir) throws IOException {
		FileWriter fw = new FileWriter(oFileDir + "Theta_" + s + ".txt");
		try {
			for (int i = 0; i < docTopicDists.length; i++) {
				fw.write(corpus.getDoc(i).docName() + "%.6f, ");
				for (int k = 0; k < docTopicDists[i].length; k++) {
					if (k == docTopicDists[i].length - 1)
						fw.write(String.format("%.6f\n", docTopicDists[i][k]));
					else
						fw.write(String.format("%.6f, ", docTopicDists[i][k]));
				}
			}
			fw.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void writePsi(int s, String oFileDir) throws IOException {
		FileWriter fw = new FileWriter(oFileDir + "Psi_" + s + ".txt");
		try {
			for (int i = 0; i < docTopicDists.length; i++) {
				for(int j = 0; j < passageTopicDists[i].length; j++){
					fw.write(corpus.getDoc(i).docName() + ", "+j+", ");
					for (int k = 0; k < docTopicDists[i].length; k++) {
						if (k == docTopicDists[i].length - 1)
							fw.write(String.format("%.6f\n", passageTopicDists[i][j][k]));
						else
							fw.write(String.format("%.6f, ", passageTopicDists[i][j][k]));
					}
				}
			}
			fw.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void writeTopicDistribution(String oFileDir){
		String folder = oFileDir + File.separator + "topicDistributions";
		(new File(folder)).mkdirs();
		try{
			FileWriter fw;
			for(int i = 0; i < docTopicDists.length; i++){
				fw = new FileWriter(folder+File.separator+corpus.getDoc(i).docName()+".dat");
				fw.write("doc-topic-distribution:\n");
				for(int k = 0; k < docTopicDists[i].length; k++){
					if(k == docTopicDists[i].length-1)
						fw.write(String.format("%.6f\n", docTopicDists[i][k]));
					else
						fw.write(String.format("%.6f, ", docTopicDists[i][k]));
				}
				fw.flush();
				fw.write("text-passage-topic-distributions (one per row):\n");
				for(int j = 0; j < passageTopicDists[i].length; j++){
					for(int k = 0; k < passageTopicDists[i][j].length; k++){
						if(k == passageTopicDists[i][j].length-1)
							fw.write(String.format("%.6f\n", passageTopicDists[i][j][k]));
						else
							fw.write(String.format("%.6f, ", passageTopicDists[i][j][k]));
					}
					fw.flush();
				}
				fw.close();
			}	
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		//write distribution in object format
		writeObject(oFileDir+File.separator+"stm_final_topic_distributions.obj");
	}
	
	/**
	 * Save topic distributions in object format.
	 * 
	 * @param fileName
	 */
	public void writeObject(String fileName){
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			//write serial version
			out.writeInt(CURRENT_SERIAL_VERSION);
			//write distribution
			for(int i = 0; i < docTopicDists.length; i++){
				for(int k = 0; k < docTopicDists[i].length; k++)
					out.writeDouble(docTopicDists[i][k]);
				for(int j = 0; j < passageTopicDists[i].length; j++){
					for(int k = 0; k < passageTopicDists[i][j].length; k++)
						out.writeDouble(passageTopicDists[i][j][k]);
				}
			}
			out.flush();out.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * Read topic distributions from a specified file.
	 * 
	 * @param fileName
	 */
	@SuppressWarnings("unused")
	public void readObject(String fileName){
		try {
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream in = new ObjectInputStream(fis);
			//read serial version
			int version = in.readInt();
			//read distributions
			for(int i = 0; i < docTopicDists.length; i++){
				for(int k = 0; k < docTopicDists[i].length; k++)
					docTopicDists[i][k] = in.readDouble();
				for(int j = 0; j < passageTopicDists[i].length; j++){
					for(int k = 0; k < passageTopicDists[i][j].length; k++)
						passageTopicDists[i][j][k] = in.readDouble();
				}
			}
			in.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
