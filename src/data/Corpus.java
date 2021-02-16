package data;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Lan Du // Mariflor Vega
 * @version 1.0 Build Dec 17, 2012
 */
public class Corpus implements Serializable {

	private Map<Integer, Document> docList = new HashMap();
	private int wordTotal;
	private int segTotal;
	private int docTotal;

	public Corpus() {
		this.docList = new HashMap();
	}

	/**
	 * Create a corpus from a given file directory
	 *
	 * @param dataDir
	 * @param voc
	 */


	public Corpus(String dataDir, Vocabulary voc ) {
		File f = new File(dataDir);
		String[] filesNames = f.list();
		int docIndex = 0;

		for(int i = 0; i < filesNames.length; ++i) {
			String fileName = filesNames[i];
			String filePath = dataDir + "/" + fileName;
			Document doc = new Document(filePath, fileName, voc);
			this.docList.put(docIndex, doc);
			this.wordTotal += doc.numWords();
			this.segTotal += doc.numSegs();

			++docIndex;
		}

		this.docTotal = this.docList.size();
        System.out.println("segTotal"+segTotal+" wordTotal: "+wordTotal);
	}


    public Corpus(String seqDir , String dataDir, Vocabulary voc ) throws IOException {
        FileReader freader = new FileReader(new File(seqDir));
        BufferedReader breader = new BufferedReader(freader);
        String fileName = breader.readLine();
        int docIndex = 0;
        while (fileName != null) {
            System.out.println(fileName);
            String filePath = dataDir + "/" + fileName;
            Document doc = new Document(filePath, fileName, voc);
            this.docList.put(docIndex, doc);
            this.wordTotal += doc.numWords();
            this.segTotal += doc.numSegs();
            ++docIndex;
            fileName = breader.readLine();
        }

        breader.close();
        this.docTotal = this.docList.size();
        System.out.println("segTotal"+segTotal+" wordTotal: "+wordTotal);
    }


	public void DocumentTheta(String dataDir) throws IOException {
	    FileReader freader = new FileReader(new File(dataDir));
        BufferedReader breader = new BufferedReader(freader);
        String text = breader.readLine();
        while (text != null) {
            String[] seqtext = text.split(", ");
            String fileName = seqtext[0];
            int docIndex = getDocIndex(fileName);
            double[] theta;
            if (docIndex != -1) {
                theta = new double[seqtext.length - 1];
                for (int j = 1; j < seqtext.length; ++j) {
                    theta[j - 1] = Double.parseDouble(seqtext[j]);
                }
                docList.get(docIndex).docTheta(theta);
            }

            text = breader.readLine();
        }

        breader.close();
	}



    public void checkTheta() {
        int D = docList.size();
        for (int d = 0; d < D; d++) {
            Document doc = getDoc(d);
            if(doc.getDocTheta()== null){
                wordTotal -= doc.numWords();
                segTotal -= doc.numSegs();
                docTotal -= 1;
                docList.remove(d);
            }
        }
        System.out.println("AFTER CHECKING DOCUMENTS WITH THETA DISTRIBUTIONS");
        System.out.println("wordTotal:"+wordTotal);
        System.out.println("segTotal:"+segTotal);
        System.out.println("docTotal:"+docTotal);
    }


	public int numWords() {
		return this.wordTotal;
	}

	public int numSegs() {
		return this.segTotal;
	}

	public int numDocs() {
		return this.docTotal;
	}

	public Document getDoc(int docIndex) {
		return this.docList.get(docIndex);
	}

	public int getDocIndex(String docName) {
        int docIndex=-1;
		for (int i = 0; i< this.docTotal; i++){
			if (this.docList.get(i).docName().equals(docName) ){
                docIndex = i;
                break;
            }
		}
        return docIndex;
	}



	/**
	 * Print the corpus statistics in console.
	 */
	public void printCorpusStats()
	{
		System.out.println("==============================================================");
		System.out.println("total number of docs: " + docList.size());
		System.out.println("total number of words: " + wordTotal);
		System.out.println("==============================================================");
	}

}
