package malletWrap;

import cc.mallet.pipe.*;
import cc.mallet.types.*;
import gnu.trove.*;

/** 
 *  Pruning low-count features based on the document frequence.
 *  <p>
 *  This code is adapted from "FeatureDocFreqPipe.java" in Mallet
 */

public class FeatureDFPipe extends Pipe 
{
		
	FeatureCounter counter;
	int numInstances;

	public FeatureDFPipe() 
	{
		super(new Alphabet(), null);
		counter = new FeatureCounter(this.getDataAlphabet());
		numInstances = 0;
	}
		
	public FeatureDFPipe(Alphabet dataAlphabet, Alphabet targetAlphabet) 
	{
		super(dataAlphabet, targetAlphabet);
		counter = new FeatureCounter(dataAlphabet);
		numInstances = 0;
	}

	public Instance pipe(Instance instance) 
	{
		TIntIntHashMap localCounter = new TIntIntHashMap();
		if (instance.getData() instanceof FeatureSequence) {
			FeatureSequence features = (FeatureSequence) instance.getData();
			for (int position = 0; position < features.size(); position++) {
				localCounter.adjustOrPutValue(features.getIndexAtPosition(position), 1, 1);
			}
		}
		else {
			throw new IllegalArgumentException("Looking for a FeatureSequence, found a " + 
											   instance.getData().getClass());
		}

		for (int feature: localCounter.keys()) {
			counter.increment(feature);
		}
		numInstances++;

		return instance;
	}

	/** 
	 *  Add all pruned words to the internal stoplist of a SimpleTokenizer.
	 * 
	 * @param docFrequencyCutoff Remove words that occur in greater than this proportion 
	 * 		of documents. 0.05 corresponds to IDF >= 3.
	 */
	public void addPrunedWordsToStoplist(SimpleTokenizer tokenizer, double docFrequencyCutoff) 
	{
		Alphabet currentAlphabet = getDataAlphabet();
        for (int feature = 0; feature < currentAlphabet.size(); feature++) {
            if ((double) counter.get(feature) / numInstances > docFrequencyCutoff) {
                tokenizer.stop((String) currentAlphabet.lookupObject(feature));
            }
        }
	}
	
	/**
	 * Return a new alphabet that contains only feacture between the specified limits.
	 * @param minDocFreq the minimum document frequency
	 * @param maxDocFreq the maximum document frequency
	 * @return prunedAlphabset 
	 */
	public Alphabet getPrunedAlphabet(int minDocFreq, int maxDocFreq)
	{
		int count;
    	Alphabet currentAlphabet = getDataAlphabet();
        Alphabet prunedAlphabet = new Alphabet();
        for (int feature = 0; feature < currentAlphabet.size(); feature++) {
        	count = counter.get(feature);
            if (count >= minDocFreq && count <= maxDocFreq) {
            	prunedAlphabet.lookupIndex(currentAlphabet.lookupObject(feature), true);
            }
        }
        prunedAlphabet.stopGrowth();
        return prunedAlphabet;
	}
	
	/**
	 * 
	 * @param minDocFreq
	 * @param maxDocFreq
	 * @return
	 */
	public Alphabet getPrunedAlphabet(double minDocFreq, double maxDocFreq)
	{
		double idf;
    	Alphabet currentAlphabet = getDataAlphabet();
        Alphabet prunedAlphabet = new Alphabet();
        
        for (int feature = 0; feature < currentAlphabet.size(); feature++) {
        	idf = (double) counter.get(feature)/numInstances;
            if (idf >= minDocFreq && idf <= maxDocFreq) {
            	prunedAlphabet.lookupIndex(currentAlphabet.lookupObject(feature), true);
            }
        }
        prunedAlphabet.stopGrowth();
        return prunedAlphabet;
	}

	static final long serialVersionUID = 1;

}
