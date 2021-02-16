package data;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mariflor Vega
 * @author Lan Du
 * @version 1.0 Build Jul 17 2020
 */
public class Document implements Serializable {
	private Map<Integer, Segment> segList = new HashMap<Integer,Segment>();
	private int Nd;
	private String docName;
	private double [] docTheta;

	public Document(){
		segList = new HashMap<Integer,Segment>();
	}

	public Document(String filePath, String fileName, Vocabulary voc) {
		this.docName = fileName;
		System.out.println(fileName);
		try {
			int segIndex = 0;
			//System.out.println("filePath:"+filePath);
			FileReader freader = new FileReader(new File(filePath));
			BufferedReader breader = new BufferedReader(freader);
			String text = breader.readLine();
			while (text != null) {
				//System.out.println("text: "+text);
				Segment seg = new Segment(text, voc);
				this.segList.put(segIndex,seg);
				this.Nd += seg.size();
				segIndex +=1;
				text = breader.readLine();
				//System.out.println("segsize: "+seg.size());
			}
			breader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public String docName() { return docName; }

	public int numWords() { return Nd; }

	public int numSegs() { return segList.size(); }

	public Segment getSegment(int key) {
		return segList.get(key);
	}

	public double[] getDocTheta() {
		return docTheta;
	}


	public void docTheta (double[] docTheta) {
		this.docTheta =docTheta;
	}


}
