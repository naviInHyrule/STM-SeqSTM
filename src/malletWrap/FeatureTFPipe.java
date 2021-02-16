package malletWrap;

import cc.mallet.pipe.*;
import cc.mallet.types.*;
import java.io.*;

/**
 * Pruning low-count feactures based on the term frequences (TF).
 * <p>
 * This code is adapted from "FeatureCountPipe.java" in Mallet.
 */
public class FeatureTFPipe extends Pipe
{	
	FeatureCounter counter;

	public FeatureTFPipe() 
	{
		super(new Alphabet(), null);
		counter = new FeatureCounter(this.getDataAlphabet());
	}
		
	public FeatureTFPipe(Alphabet dataAlphabet, Alphabet targetAlphabet) 
	{
		super(dataAlphabet, targetAlphabet);
		counter = new FeatureCounter(dataAlphabet);
	}

	public Instance pipe(Instance instance) 
	{		
		if (instance.getData() instanceof FeatureSequence) {			
			FeatureSequence features = (FeatureSequence) instance.getData();
			for (int position = 0; position < features.size(); position++) {
				counter.increment(features.getIndexAtPosition(position));
			}
		}
		else {
			throw new IllegalArgumentException("Looking for a FeatureSequence, found a " + 
											   instance.getData().getClass());
		}
		return instance;
	}

	/**
	 * Returns a new alphabet that contains only features at or above 
	 *  the specified limit.
	 */
	public Alphabet getPrunedAlphabet(int minimumCount) 
	{		
		Alphabet currentAlphabet = getDataAlphabet();
		Alphabet prunedAlphabet = new Alphabet();
		for (int feature = 0; feature < currentAlphabet.size(); feature++) {
			if (counter.get(feature) >= minimumCount) {
				prunedAlphabet.lookupIndex(currentAlphabet.lookupObject(feature), true);
			}
		}
		prunedAlphabet.stopGrowth();
		return prunedAlphabet;
	}
	/**
	 * Return a new alphabet that contains only feacture between the specified limits,
	 * @param minimumCount 
	 * @param maximumCount
	 * @return
	 */
	 public Alphabet getPrunedAlphabet(int minimumCount, int maximumCount)
	    {
		 	int count;
	    	Alphabet currentAlphabet = getDataAlphabet();
	        Alphabet prunedAlphabet = new Alphabet();
	        for (int feature = 0; feature < currentAlphabet.size(); feature++) {
	        	count = counter.get(feature);
	            if (count >= minimumCount && count <= maximumCount) {
	            	prunedAlphabet.lookupIndex(currentAlphabet.lookupObject(feature), true);
	            }
	        }
	        prunedAlphabet.stopGrowth();
	        return prunedAlphabet;
	    }

	/** 
	 *  Writes a list of features that do not occur at or 
	 *  above the specified cutoff to the pruned file, one per line.
	 *  This file can then be passed to a stopword filter as 
	 *  "additional stopwords".
	 */
	public void writePrunedWords(File prunedFile, int minimumCount) throws IOException 
	{
		PrintWriter out = new PrintWriter(prunedFile);
		Alphabet currentAlphabet = getDataAlphabet();
		for (int feature = 0; feature < currentAlphabet.size(); feature++) {
			if (counter.get(feature) < minimumCount) {
				out.println(currentAlphabet.lookupObject(feature));
			}
		}
		out.close();
	}
	/**
	 * Write a list of feactures that od not occur above the mini-cutoff
	 * or below the maxi-cutoff.
	 * @param prunedFile
	 * @param minimumCount
	 * @param maximumCount
	 * @throws IOException
	 */
	public void writePrunedWords(File prunedFile, int minimumCount, int maximumCount) throws IOException 
	{
		int count;
		PrintWriter out = new PrintWriter(prunedFile);
		Alphabet currentAlphabet = getDataAlphabet();
		for (int feature = 0; feature < currentAlphabet.size(); feature++) {
			count = counter.get(feature);
			if (count < minimumCount || count > maximumCount) {
				out.println(currentAlphabet.lookupObject(feature));
			}
		}
		out.close();
	}

	/** 
	 *  Add all pruned words to the internal stoplist of a SimpleTokenizer.
	 */
	public void addPrunedWordsToStoplist(SimpleTokenizer tokenizer, int minimumCount) 
	{
		Alphabet currentAlphabet = getDataAlphabet();
        for (int feature = 0; feature < currentAlphabet.size(); feature++) {
            if (counter.get(feature) < minimumCount) {
                tokenizer.stop((String) currentAlphabet.lookupObject(feature));
            }
        }
	}
	/**
	 * Add all pruned word to interanl stoplist of a SimpleTokenizer. Those pruned
	 * words have frequencey above the max-cutoff or below the min-cutoff.
	 * @param tokenizer
	 * @param minimumCount
	 * @param maximumCount
	 */
	public void addPrunedWordsToStoplist(SimpleTokenizer tokenizer, int minimumCount, int maximumCount) 
	{
		int count;
		Alphabet currentAlphabet = getDataAlphabet();
        for (int feature = 0; feature < currentAlphabet.size(); feature++) {
        	count = counter.get(feature);
            if (count < minimumCount || count > maximumCount) {
                tokenizer.stop((String) currentAlphabet.lookupObject(feature));
            }
        }
	}

	/**
	 * List the most common words, for addition to a stop file
	 */
	public void writeCommonWords(File commonFile, int totalWords) throws IOException 
	{
		PrintWriter out = new PrintWriter(commonFile);
        Alphabet currentAlphabet = getDataAlphabet();

		IDSorter[] sortedWords = new IDSorter[currentAlphabet.size()];
		for (int type = 0; type < currentAlphabet.size(); type++) {
			sortedWords[type] = new IDSorter(type, counter.get(type));
		}
		java.util.Arrays.sort(sortedWords);

		int max = totalWords;
		if (currentAlphabet.size() < max) {
			max = currentAlphabet.size();
		}

		for (int rank = 0; rank < max; rank++) {
			int type = sortedWords[rank].getID();
			out.println (currentAlphabet.lookupObject(type));
		}
		out.close();
	}
	
	static final long serialVersionUID = 1;
}
