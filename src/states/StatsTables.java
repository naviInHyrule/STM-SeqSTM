package states;

import java.io.*;
import data.*;

/**
 * 
 * @author Lan Du
 * @version 2013-1-11 11:25
 */
public class StatsTables implements Serializable {
	// Serialization
	private static final long serialVersionUID = 1L;
	private static final int CURRENT_SERIAL_VERSION = 0;
	// private class variables
	private final Corpus corpus;
	private final int numTopics;
	private final int numTypes;
	// model level stats
	public int[][] MKW;
	public int[] MK;
	// document level stats
	public int[][][] TIJK;
	public int[][] TIK;
	public int[][] TIJ;
	public int[] TI;
	public int[][][] NIJK;
	public int[][] NIJ;

	/**
	 * 
	 * @param numTopics
	 *            the number of topics
	 * @param numTypes
	 *            number of word types
	 * @param corpus
	 *            a set of documents
	 */
	public StatsTables(int numTopics, int numTypes, Corpus corpus) {
		this.corpus = corpus;
		this.numTopics = numTopics;
		this.numTypes = numTypes;
		MKW = new int[numTopics][numTypes];
		MK = new int[numTopics];
		int numDocs = corpus.numDocs();
		TI = new int[numDocs];
		TIK = new int[numDocs][numTopics];
		TIJ = new int[numDocs][];
		TIJK = new int[numDocs][][];
		NIJK = new int[numDocs][][];
		NIJ = new int[numDocs][];
		int numTPs;
		for (int i = 0; i < numDocs; i++) {
			numTPs = corpus.getDoc(i).numSegs();
			TIJ[i] = new int[numTPs];
			TIJK[i] = new int[numTPs][numTopics];
			NIJ[i] = new int[numTPs];
			NIJK[i] = new int[numTPs][numTopics];
		}
	}

	public void clean() {
		TI = null;
		TIK = null;
		TIJ = null;
		TIJK = null;
		NIJ = null;
		NIJK = null;
	}

	public void clean(int i, int j) {
		TI[i] = 0;
		TIK[i] = new int[numTopics];
		TIJ[i][j]= 0;
		TIJK[i][j] = new int[numTopics];
		NIJ[i][j] = 0 ;
		NIJK[i][j] = new int[numTopics];
	}

	/**
	 * Check the constraints on the recorded statistics.
	 */
	public void checkInvariance() {
		// Corpus level check
		int sumMKW, sumMK = 0;
		for (int k = 0; k < MKW.length; k++) {
			sumMKW = 0;
			for (int w = 0; w < MKW[k].length; w++) {
				assert MKW[k][w] >= 0 : "MKW[" + k + "][" + w + "] = "
						+ MKW[k][w];
				sumMKW += MKW[k][w];
			}
			assert MK[k] >= 0 : "MK[" + k + "] = " + MK[k];
			assert MK[k] == sumMKW : "MK[" + k + "] = " + MK[k] + ", Sum-MKW["
					+ k + "] = " + sumMKW;
			sumMK += MK[k];
		}
		assert sumMK == corpus.numWords();
		// Document level check
		for (int i = 0; i < NIJ.length; i++) {
			int sumTIK = 0;
			for (int k = 0; k < TIK[i].length; k++) {
				int sumTIJK = 0;
				for (int j = 0; j < TIJ[i].length; j++) {
					assert TIJK[i][j][k] >= 0;
					sumTIJK += TIJK[i][j][k];
				}
				assert sumTIJK == TIK[i][k];
				sumTIK += TIK[i][k];
			}
			assert sumTIK == TI[i];

			int sumTIJ = 0;
			for (int j = 0; j < NIJ[i].length; j++) {
				int sumNIJK = 0, sumTIJK = 0;
				for (int k = 0; k < NIJK[i][j].length; k++) {
					assert NIJK[i][j][k] >= TIJK[i][j][k];
					if (NIJK[i][j][k] == 0)
						assert TIJK[i][j][k] == 0;
					if (TIJK[i][j][k] == 0)
						assert NIJK[i][j][k] == 0;
					sumNIJK += NIJK[i][j][k];
					sumTIJK += TIJK[i][j][k];
				}
				assert sumNIJK == NIJ[i][j];
				assert NIJ[i][j] == corpus.getDoc(i).getSegment(j).size();
				assert sumTIJK == TIJ[i][j];
				sumTIJ += TIJ[i][j];
			}
			assert sumTIJ == TI[i];
		}
	}

	/**
	 * Adjust the customer (word) counts
	 * 
	 * @param i
	 *            document index
	 * @param j
	 *            text passage index
	 * @param w
	 *            word index
	 * @param k
	 *            topic index
	 * @param val
	 *            amount to change
	 */
	public void adjustCust(final int i, final int j, final int w, final int k,
			final int val) {
		MKW[k][w] += val;
		MK[k] += val;
		NIJK[i][j][k] += val;
		NIJ[i][j] += val;
		if (Parameters.debug) {
			assert MKW[k][w] >= 0;
			assert MK[k] >= 0;
			assert NIJK[i][j][k] >= 0;
			assert NIJ[i][j] >= 0;
		}
	}

	/**
	 * Adjust the table count by the given amount.
	 * 
	 * @param i
	 *            documnet index
	 * @param j
	 *            text passage index
	 * @param k
	 *            topic index
	 * @param val
	 *            amount to change
	 */
	public void adjustTable(final int i, final int j, final int k, final int val) {
		TIJK[i][j][k] += val;
		TIJ[i][j] += val;
		TIK[i][k] += val;
		TI[i] += val;
		if (Parameters.debug) {
			assert TIJK[i][j][k] >= 0;
			assert TIJ[i][j] >= 0;
			assert TIK[i][k] >= 0;
			assert TI[i] >= 0;
		}
	}

	/**
	 * Save topic-by-word count matrix in format required by topic evaluation.
	 */
	public void saveTopicByWordCountMatrix(String file, Vocabulary voc) {
		try {
			FileWriter fw = new FileWriter(file);
			for (int k = 0; k < MKW.length; k++) {
				for (int w = 0; w < MKW[k].length; w++)
					fw.write(String.format("%d 1 t_%d %s\n", MKW[k][w],
							(k + 1), voc.getType(w)));
				fw.flush();
			}
			fw.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Save statistics.
	 * 
	 * @param fileName
	 */
	public void writeObject(String fileName) {
		try {
			System.out.println("writing object");
			FileOutputStream fos = new FileOutputStream(fileName);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			// write serial version
			out.writeInt(CURRENT_SERIAL_VERSION);
			// write document level stats
			for (int k = 0; k < numTopics; k++) {
				out.writeInt(MK[k]);
				for (int w = 0; w < numTypes; w++)
					out.writeInt(MKW[k][w]);
			}
			// write TI, TIJ, TIJK, NIJ, NIJK
			for (int i = 0; i < corpus.numDocs(); i++) {
				out.writeInt(TI[i]);
				for (int j = 0; j < corpus.getDoc(i).numSegs(); j++) {
					out.writeInt(TIJ[i][j]);
					out.writeInt(NIJ[i][j]);
					for (int k = 0; k < numTopics; k++) {
						out.writeInt(NIJK[i][j][k]);
						out.writeInt(TIJK[i][j][k]);
					}
				}
			}
			// wirte TIK
			for (int i = 0; i < corpus.numDocs(); i++)
				for (int k = 0; k < numTopics; k++)
					out.writeInt(TIK[i][k]);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read StatsTables object from a file.
	 * 
	 * @param fileName
	 * @return
	 */
	@SuppressWarnings("unused")
	public void readObject(String fileName) {
		try {
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream in = new ObjectInputStream(fis);
			// read serial version
			int version = in.readInt();
			// read MK and MKW
			for (int k = 0; k < numTopics; k++) {
				MK[k] = in.readInt();
				for (int w = 0; w < numTypes; w++)
					MKW[k][w] = in.readInt();
			}
			// read TI, TIJ, TIJK, NIJ, NIJK
			for (int i = 0; i < corpus.numDocs(); i++) {
				TI[i] = in.readInt();
				for (int j = 0; j < corpus.getDoc(i).numSegs(); j++) {
					TIJ[i][j] = in.readInt();
					NIJ[i][j] = in.readInt();
					for (int k = 0; k < numTopics; k++) {
						NIJK[i][j][k] = in.readInt();
						TIJK[i][j][k] = in.readInt();
					}
				}
			}
			// read TIK
			for (int i = 0; i < corpus.numDocs(); i++)
				for (int k = 0; k < numTopics; k++)
					TIK[i][k] = in.readInt();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
