package states;

import java.io.*;

import data.*;

/**
 * 
 * @author Lan Du
 * @since 2013-1-11
 */
public class TopicsAss implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final int CURRENT_SERIAL_VERSION = 0;

	private final Corpus corpus;
	private int[][][] topicAssignment;

	/**
	 * @param c
	 *            Corpus
	 */
	public TopicsAss(final Corpus c) {
		corpus = c;
		topicAssignment = new int[c.numDocs()][][];
		for (int i = 0; i < c.numDocs(); i++) {
			Document doc = c.getDoc(i);
			topicAssignment[i] = new int[doc.numSegs()][];
			for (int j = 0; j < doc.numSegs(); j++)
				topicAssignment[i][j] = new int[doc.getSegment(j).size()];
		}
	}

	/**
	 * Create topic assignment object with given topic assignments.
	 * 
	 * @param topics
	 *            prespecified topic assignments, a three-dimensional int array.
	 */
	public TopicsAss(int[][][] topics, final Corpus c) {
		this(c);
		for (int i = 0; i < topics.length; i++)
			for (int j = 0; j < topics[i].length; j++)
				for (int n = 0; n < topics[i][j].length; n++)
					topicAssignment[i][j][n] = topics[i][j][n];
	}

	/**
	 * Return topic assignments of all the words
	 * 
	 * @return
	 */
	public int[][][] getAllTopics() {
		return topicAssignment;
	}

	/**
	 * @param i
	 *            the i-th document
	 * @param j
	 *            the j-th text passage in i
	 * @param n
	 *            the n-th word in j
	 * @param k
	 *            topic assignment
	 */
	public void setTopic(int i, int j, int n, int k) {
		topicAssignment[i][j][n] = k;
	}

	/**
	 * 
	 * @param i
	 *            the i-th document
	 * @param j
	 *            the j-th text passage in i
	 * @param n
	 *            the n-th word in j
	 * @return
	 */
	public int getTopic(int i, int j, int n) {
		return topicAssignment[i][j][n];
	}

	/**
	 * Save words and their topic assignments.
	 * 
	 * @param ofileDir
	 */
	public void writeWordAndTopic(final String ofileDir, Vocabulary voc) {
		FileWriter fw;
		String folder = ofileDir + File.separator + "topicAssignment";
		(new File(folder)).mkdirs();
		try {
			for (int i = 0; i < topicAssignment.length; i++) {
				fw = new FileWriter(folder + File.separator
						+ corpus.getDoc(i).docName() + ".dat");
				for (int j = 0; j < topicAssignment[i].length; j++) {
					for (int n = 0; n < topicAssignment[i][j].length; n++) {
						fw.write(String.format(
								"%s(%d)",
								voc.getType(corpus.getDoc(i).getSegment(j)
										.getWord(n)), topicAssignment[i][j][n]));
						if (n < topicAssignment[i][j].length - 1)
							fw.write(", ");
					}
					fw.write("\n\n");
					fw.flush();
				}
				fw.close();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		// write topic assignment in object format
		writeObject(ofileDir + File.separator
				+ "stm_final_topic_Assignments.obj");
	}

	/**
	 * Save topic assingments in obj file
	 * 
	 * @param fileName
	 */
	public void writeObject(String fileName) {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeInt(CURRENT_SERIAL_VERSION);
			for (int i = 0; i < topicAssignment.length; i++)
				for (int j = 0; j < topicAssignment[i].length; j++)
					for (int n = 0; n < topicAssignment[i][j].length; n++)
						oos.writeInt(topicAssignment[i][j][n]);
			oos.flush();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read topic assignments from a obj file.
	 * 
	 * @param fileName
	 * @return
	 */
	@SuppressWarnings("unused")
	public void readObject(String fileName) {
		try {
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			int version = ois.readInt();
			for (int i = 0; i < topicAssignment.length; i++)
				for (int j = 0; j < topicAssignment[i].length; j++)
					for (int n = 0; n < topicAssignment[i][j].length; n++)
						topicAssignment[i][j][n] = ois.readInt();
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
