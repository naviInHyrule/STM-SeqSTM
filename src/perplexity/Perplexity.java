package perplexity;

import data.Corpus;
import data.Document;
import data.Segment;
import util.MTRandom;

import java.io.*;

/**
 *
 * @author mariflor vega
 *
 */
public class Perplexity {
	/*
	 * Variables for testing
	 */
	private Corpus docs;
	private int R,D,T,S;
	private MTRandom rng;
	private double[][] LLK;
	private double[][] Ns;
	private TopicDistributions  Phi;

	/*
	 * Variable for log files
	 */
	private FileWriter llihoodWriter, perpWriter, bFileWriter;
	private FileWriter alphaWriter, gammaWriter;

	/**
	 * Train the STM, and test STM if testing documents are given.
	 *

	 * @param Phi word distributions
	 * @param docs data with document-specific topic distributions
	 * @param R     particles
	 */
	public Perplexity(TopicDistributions Phi, Corpus docs ,int R) throws IOException {
		this.R = R;
		this.docs = docs;
		this.T = Phi.length();
		this.Phi=Phi;
		System.out.println("Number of topics:"+T);
		System.out.println("Number of particles:"+R);
	}



	public double run(String root) throws FileNotFoundException, UnsupportedEncodingException {
		D=docs.numDocs();
		S=docs.numSegs();
		LLK = new double[D][];
		Ns = new double[D][];
		double llk = 0.0;
		double perp;
		int Nd;

		for (int d = 0; d < D; d++) {

			Document doc = docs.getDoc(d);
			Nd=doc.numSegs();
			LLK[d] = new double[doc.numSegs()];
			Ns[d] = new double[doc.numSegs()];
			Prior Theta = new Prior(doc.getDocTheta());
			System.out.println("theta[0]"+Theta.getParam(0));
			for (int s = 0; s < Nd; s++) {
				Segment seg = doc.getSegment(s);
				LLK[d][s] = LeftToRight(seg, Theta, Phi);
				Ns[d][s] = seg.size();
				//System.out.println("----d:"+d+" Nd:" + docs.getDoc(d).size()+" llk:"+LLK[d]);
				llk += LLK[d][s];
			}
		}

		perp=-1*llk/docs.numWords();
		System.out.println("llk:"+llk+", perp:"+perp+", N:"+docs.numWords());
		if(root !=null) {
			PrintWriter logProbWriter = new PrintWriter(root + "llk.txt", "UTF-8");

			for (int d = 0; d <D; d++) {
				Nd=docs.getDoc(d).numSegs();
				for (int s = 0; s < Nd; s++) {
					logProbWriter.println(LLK[d][s] + "," + Ns[d][s]);
					logProbWriter.flush();
				}
			}

			PrintWriter perpWriter = new PrintWriter(root + "perp.txt", "UTF-8");
			perpWriter.println(perp);
			perpWriter.flush();
		}

		return(perp);

	}

	public double LeftToRight(Segment seg, Prior theta, TopicDistributions Phi) throws FileNotFoundException, UnsupportedEncodingException {

		rng = new MTRandom();
		double logllk = 0.0;
		int Nd= seg.size();

		double thetaSum= theta.sumParam();

		int[] Ns ;
		int[] ss ;
		double[] ps = new double[T];
		double pn;
		int ss_t;

		for (int n = 0; n < Nd; n++) {
			pn = 0.0;
			//System.out.println("w"+doc.getWord(n));
			for (int r = 0; r < R; r++) {
				Ns = new int[T];
				ss = new int[Nd];
				for (int n_ = 0; n_ < n; n_++) {
					//System.out.println("w_"+doc.getWord(n_));
					for (int k = 0; k < T; k++) {

						ps[k] = Phi.getScore(k,seg.getWord(n_)) * (Ns[k] + theta.getParam(k)) / (sum(Ns) + theta.sumParam());
					}
					ss_t = rng.nextDiscrete(ps);
					ss[n_] = ss_t;
					Ns[ss_t] += 1;
					//System.out.println("n: "+n+" r: "+r+ " n_: "+n_+" NS: "+sum(Ns));
					//for(int j = 0; j < ss.length; j++){
					//    System.out.print(ss[j]+",");
					//}
					//System.out.println();
				}
				for (int k = 0; k < T; k++) {

					ps[k] = Phi.getScore(k,seg.getWord(n)) * (Ns[k] + theta.getParam(k)) / (sum(Ns) + theta.sumParam());
					pn += ps[k];
					//System.out.println("phiw: "+Phi.getScore(k, n)+" alphaSum: "+alphaSum+ " alpha: "+alpha+" NS: "+sum(Ns)+" pn:"+pn);
				}

			}

			pn /= R;
			logllk += Math.log(pn);
			//System.out.println("--n: "+n+ " pn: "+ pn);

		}

		return logllk;
	}


	public int sum(int[] vector) {
		int suma = 0;
		for (int i = 0; i < vector.length; i++) {
			suma += vector[i];
		}
		return suma;
	}
}
