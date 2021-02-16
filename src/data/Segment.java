package data;

import gnu.trove.TIntArrayList;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author Mariflor Vega
 * @author Lan Du
 * @version 1.0 Build Jul 17 2020
 */
public class Segment implements Serializable {
	private final String text;
	private ArrayList<Integer> words;

	/**
	 * Build a text passage.
	 *
	 * @param text
	 *            the original text
	 * @param voc
	 *            the vocabulary
	 */

	public Segment(String text, Vocabulary voc) {
		this.text = text;
		this.words = new ArrayList<Integer>();
		String[] tokens = text.split("\\|");
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			if (token.length() >= 1 && voc.contains(token)) {
				words.add(voc.getTypeIndex(token).intValue());
			}
		}
	}

	/**
	 *
	 * @param words an int arraylist of word indices
	 * @param text
	 */

	public Segment(ArrayList words, String text) {
		this.text = text;
		this.words = words;
	}

	/**
	 * 
	 * @return the text content.
	 */
	public String text() {
		return text;
	}

	/**
	 * 
	 * @return the size of the tokenized text passage.
	 */
	public int size() {
		return words.size();
	}



	/**
	 * Get the word in the specified position in the text.
	 * 
	 * @param n
	 *            the word position in a text passage
	 * @return the type index
	 */
	public int getWord(int n) {
		return words.get(n);
	}

	/**
	 * 
	 * @return the hash code of this text passage.
	 */
	public int getHashCode() {
		return this.hashCode();
	}


}
