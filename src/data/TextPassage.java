package data;

import java.io.Serializable;
import gnu.trove.TIntArrayList;

/**
 * 
 * @author Lan Du
 * @version 1.0 Build Dec 17, 2012
 */
public class TextPassage implements Serializable {
	//Serialization
	private static final long serialVersionUID = 1L;
	//
	private final int tpIndex;
	private final String text;
	private TIntArrayList words;

	/**
	 * Build a text passage.
	 * 
	 * @param text
	 *            the original text
	 * @param index
	 *            the index of the text passage in the document
	 * @param voc
	 *            the vocabulary
	 */
	public TextPassage(String text, int tpIndex, Vocabulary voc) {
		this.text = text;
		this.tpIndex = tpIndex;
		words = new TIntArrayList();
		String[] tokens = text.split("[\\W0-9]");
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i].toLowerCase();
			if (token.length() > 1 && voc.contains(token)) {
				words.add(voc.getTypeIndex(token).intValue());
			}
		}
	}
	/**
	 * 
	 * @param words an int arraylist of word indices
	 * @param tpIndex the passage index
	 * @param text
	 */
	public TextPassage(TIntArrayList words, int tpIndex, String text) {
		this.tpIndex = tpIndex;
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
	 * Return the index of text passage in a document.
	 * @return 
	 */
	public int getTPIndex() {
		return tpIndex;
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
