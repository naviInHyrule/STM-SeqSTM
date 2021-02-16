package util;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * 
 * @author Lan Du
 *
 */
public class MTRandom {

	private static MersenneTwister rand = new MersenneTwister();

	/**
	 * Reinitialize the generator as if just built with the given long seed.
	 * 
	 * @param seed
	 */
	public static void setSeed(long seed) {
		rand.setSeed(seed);
	}

	/**
	 * Returns the next pseudorandom, uniformly distributed double value between
	 * 0.0 and 1.0.
	 * 
	 * @return
	 */
	public static double nextDouble() {
		return rand.nextDouble();
	}

	/**
	 * Returns a pseudorandom, uniformly distributed int value between 0
	 * (inclusive) and the specified value (exclusive)
	 * 
	 * @param n
	 * @return
	 */
	public static int nextInt(int n) {
		return rand.nextInt(n);
	}

	public static boolean nextBoolean() {
		return rand.nextBoolean();
	}

	public static RandomGenerator generator() {
		return rand;
	}

	public int nextDiscrete(double[] probs) {
		double distSum = 0.0;
		for(int p = 0; p < probs.length; p++){
			distSum += probs[p];
		}

		double sum = 0.0;
		double r =  nextDouble() * distSum;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
			if (sum > r)  return i;
		}
		return probs.length - 1;
	}
}
