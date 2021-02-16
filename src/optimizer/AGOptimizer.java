package optimizer;

import org.apache.commons.math3.special.Gamma;

public class AGOptimizer {

	private static final int MAXITE = 100;
	private static final double THRESHOLD = 1e-4;
	private static final double MAX = 10;
	private static final double MIN = 0.0001;

	/**
	 * @param counts
	 *            double[size1][size2]
	 * @param count_sum
	 *            double[size1]
	 * @param size1
	 *            #docs for alpha, #topics for gamma
	 * @param size2
	 *            #topic for alpha, #types for gamma
	 * @param intV
	 *            the previous alpha value or gamma value
	 * @return
	 */
	public static double sym_polya_fit(double[][] counts, 
									   double[] count_sum,
									   int size1, int size2, 
									   double intV) 
	{
		double sum1, sum2, oldValue, newValue;
		int iter = 0;

		newValue = oldValue = intV;
		while (iter++ < MAXITE) {
			sum1 = 0;
			sum2 = 0;
			oldValue = newValue;
			for (int i = 0; i < size1; i++) {
				for (int j = 0; j < size2; j++) {
					sum1 += Gamma.digamma(counts[i][j] + oldValue);
				}
				sum2 += Gamma.digamma(count_sum[i] + size2 * oldValue);
			}
			sum1 -= size1 * size2 * Gamma.digamma(oldValue);
			sum2 *= size2;
			sum2 -= size1 * size2 * Gamma.digamma(size2 * oldValue);
			newValue = oldValue * sum1 / sum2;
			if (newValue > MAX) {
				newValue = 0.5;
				break;
			}
			if (newValue < MIN) {
				newValue = MIN;
				break;
			}
			if (iter > 20 && Math.abs(newValue - oldValue) / oldValue < THRESHOLD) {
				break;
			}
		}
		return newValue;
	}

	/**
	 * @param counts
	 *            double[size1][size2]
	 * @param count_sum
	 *            double[size1]
	 * @param size1
	 *            #docs for alpha, #topics for gamma
	 * @param size2
	 *            #topic for alpha, #types for gamma
	 * @param intV
	 *            the previous alpha value or gamma value
	 * @return
	 */
	public static double sym_polya_fit_newton(double[][] counts,
											  double[] count_sum, 
											  int size1, int size2, 
											  double initV) 
	{
		int k, v, iter;
		double sum1, sum2, oldValue, newValue;
		iter = 0;
		newValue = oldValue = initV;
		while (iter++ < MAXITE) {
			sum1 = 0;
			sum2 = 0;
			oldValue = newValue;
			for (k = 0; k < size1; k++) {
				for (v = 0; v < size2; v++) {
					sum1 += Gamma.digamma(counts[k][v] + oldValue);
					sum2 += Gamma.trigamma(counts[k][v] + oldValue);
				}
				sum1 -= size2 * Gamma.digamma(count_sum[k] + size2 * oldValue);
				sum2 -= (double) size2 * (double) size2
						* Gamma.trigamma(count_sum[k] + size2 * oldValue);
			}
			sum1 += size1
					* size2
					* (Gamma.digamma(size2 * oldValue) - Gamma
							.digamma(oldValue));
			sum2 += size1
					* size2
					* (size2 * Gamma.trigamma(size2 * oldValue) - Gamma
							.trigamma(oldValue));
			newValue -= sum1 / sum2;
			/*
			if (newValue > MAX) {
				newValue = 0.5;
				break;
			}
			if (newValue < MIN) {
				newValue = MIN;
				break;
			}*/
			if (iter > 20 && Math.abs(newValue - oldValue) / oldValue < THRESHOLD) {
				break;
			}
		}
		return newValue;
	}
}
