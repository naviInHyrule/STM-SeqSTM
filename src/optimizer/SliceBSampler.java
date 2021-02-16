package optimizer;

import org.apache.commons.math3.random.RandomGenerator;

import states.Parameters;
import states.StatsTables;
import util.SpecialFuns;

public class SliceBSampler extends SliceSampler {

	private static final double BMIN = 0.001;
	private static final double BMAX = 5000;
	private boolean doOneB;
	private StatsTables stables;
	private int docID;
	private final double a;

	public SliceBSampler(StatsTables stables,
						 double a,
						 RandomGenerator rand) 
	{
		super(rand);
		this.a = a;
		this.stables = stables;
	}
	
	public SliceBSampler(StatsTables stables, double a) {
		super();
		this.a = a;
		this.stables = stables;
	}

	public double logpdf(double b, Object params) {
		double val = 0;
		if(doOneB){
			for(int i = 0; i < stables.TIJ.length; i++)
				for(int j = 0; j < stables.TIJ[i].length; j++){
					val += SpecialFuns.logPochSym(b, a, stables.TIJ[i][j])
							- SpecialFuns.logPochSym(b, 1.0, stables.NIJ[i][j]);
				}
		}else{
			for(int j = 0; j < stables.TIJ[docID].length; j++){
				val += SpecialFuns.logPochSym(b, a, stables.TIJ[docID][j])
						- SpecialFuns.logPochSym(b, 1.0, stables.NIJ[docID][j]);
			}
		}
		return val;
	}
	/**
	 * 
	 * @param oldBi
	 * @param docID
	 * @return
	 */
	public double sample(double oldBi, final int docID) {
		this.doOneB = false;
		this.docID = docID;
		double newBi = sliceSample1D(null, oldBi, BMIN, BMAX, oldBi / 32.0, 10, 32);
		if (Parameters.verboseLevel >= 7000) 
				System.out.printf("slice ===> i = %d, old b: %.2f, new b: %.2f\n", docID, oldBi, newBi);
		return newBi;
	}
	
	public double sample(double oldBi) {
		this.doOneB = true;
		double newBi = sliceSample1D(null, oldBi, BMIN, BMAX, oldBi / 32.0, 10, 32);
		if (Parameters.verboseLevel >= 7000) 
				System.out.printf("slice ===> old b: %.2f, new b: %.2f\n", oldBi, newBi);
		return newBi;
	}
	
}