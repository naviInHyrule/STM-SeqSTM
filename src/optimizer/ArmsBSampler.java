package optimizer;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.special.Gamma;

import states.Parameters;
import states.StatsTables;
import util.MTRandom;

public class ArmsBSampler extends ArmSampler
{
	private static final double BMIN = 0.01;
	private static final double BMAX = 5000;
	
	private final double scale;
	private final double shape;
	private final double a;
	
	private boolean doOneB;
	private StatsTables stables;
	private double lgqSum;
	private int docID;
	
	public ArmsBSampler(StatsTables stables,
						double a,
						double scale, 
						double shape)
	{
		this.stables = stables;
		this.a = a;
		this.scale = scale; 
		assert scale > 0;
		this.shape = shape;
	}
	
	public double logpdf(double b, Object params)
	{
		double val = (lgqSum - 1.0/scale) * b + (shape - 1.0)*Math.log(b);
		double ba = b/a;
		double lggba = Gamma.logGamma(ba);
		if(doOneB){
			for(int i = 0; i < stables.TIJ.length; i++)
				for(int j = 0; j < stables.TIJ[i].length; j++)
					val += Gamma.logGamma(ba + stables.TIJ[i][j]) - lggba;
		} else {
			for(int j = 0; j < stables.TIJ[docID].length; j++)
				val += Gamma.logGamma(ba + stables.TIJ[docID][j]) - lggba;
		}
		return val;
	}
	
	private double runSample(double oldB){
		double newB = 0;
		if(a == 0){
			double totalT = shape;
			if(doOneB){
				for(int i = 0; i < stables.TIJ.length; i++)
					for(int j = 0; j < stables.TIJ[i].length; j++)
						totalT += stables.TIJ[i][j];
			}else{
				for(int j = 0; j < stables.TIJ[docID].length; j++)
					totalT += stables.TIJ[docID][j];
			}
			if(totalT > 400){
				NormalDistribution gaussian = new NormalDistribution(MTRandom.generator(),
																	 0, 1.0,
											 NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
				do{
					newB = totalT + gaussian.sample()*Math.sqrt(totalT);
				}while(newB <= 0);
			}else{
				GammaDistribution gammaDist = new GammaDistribution(MTRandom.generator(), 
																	totalT, 1.0,
										GammaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
				newB = gammaDist.sample();
				newB /= 1.0/scale - lgqSum;
			}
			if(newB < BMIN)
				newB = BMIN;
			if(newB > BMAX)
				newB = BMAX;
		}else{
			double initb = oldB;
			if(Math.abs(initb-BMAX)/BMAX < 0.00001)
				initb = BMAX*0.999 + BMIN*0.001;
			if(Math.abs(initb-BMIN)/BMIN < 0.00001)
				initb = BMIN*0.999 + BMAX*0.001;
			double[] xl = {BMIN};
			double[] xr = {BMAX};
			double[] xprev = {initb};
			try{
				newB = armsSimple(null, 8, xl, xr, true, xprev);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return newB;
	}
	
	/**
	 * Sample concetration parameter for each document.
	 * 
	 * @param oldB the current concentration parameter
	 * @param docID document index
	 * @return
	 */
	public double sample(double oldB, int docID){
		this.docID = docID;
		this.doOneB = false;
		lgqSum = 0;
		for(int j = 0; j < stables.NIJ[docID].length; j++) {
			BetaDistribution betaDist = new BetaDistribution(MTRandom.generator(), 
															 oldB, stables.NIJ[docID][j], 
										BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
			double q = 0;
			int ite = 50;
			while(q <= 0 && ite-- > 0)
				q = betaDist.sample();
			
			assert q > 0 : "Error: q <= 0 !!! q = "+q+", bi = "+oldB+", " +
					"NTIJ = "+stables.NIJ[docID][j];
			
			if(q < 1e-10)
				q = 1e-10;
			lgqSum += Math.log(q);
		}
		double newB = runSample(oldB);
		if(Parameters.verboseLevel >= 7000)
			System.out.printf("Arms ===> i = %d, oldb: %.2f, newb: %.2f\n", docID, oldB, newB);
		return newB;
	}
	
	/**
	 * Sample a concentration parameter for the whole 
	 * corpus, and return the sampled value.
	 * @param oldB
	 * @return
	 */
	public double sample(double oldB){
		this.docID = -1;
		this.doOneB = true;
		lgqSum = 0;
		for(int i = 0; i < stables.NIJ.length; i++) {
			for(int j = 0; j < stables.NIJ[i].length; j++) {
				//System.out.println("oldB" +oldB + " nij: "+stables.NIJ[i][j]);
				BetaDistribution betaDist = new BetaDistribution(MTRandom.generator(), 
																 oldB, stables.NIJ[i][j], 
									BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
				double q = 0;
				int ite = 50;
				while(q <= 0 && ite-- > 0)
					q = betaDist.sample();
				
				assert q > 0 : "Error: q <= 0 !!! q = "+q+", bi = "+oldB+", " +
						"NTIJ = "+stables.NIJ[i][j];
				
				if(q < 1e-10)
					q = 1e-10;
				lgqSum += Math.log(q);
			}
		}
		double newB = runSample(oldB);
		if(Parameters.verboseLevel >= 7000)
			System.out.printf("Arms ===> oldb: %.2f, newb: %.2f\n", oldB, newB);
		return newB;
	}
}
