package util;

import java.io.*;
import java.util.Vector;

/**
 * 
 * @author Lan Du
 *
 */
public class StirNum {
	public static final int EXPSIZE = 200;
	private static int maxN;
	private static int maxM;
	private static double a;
	private static Vector<Float> stirlingTable;

	/**
	 * Set the parameters used in building the Stiriling table.
	 *
	 * @param N
	 * @param M
	 * @param val
	 */
	public static void initialize(int N, int M, double val) {
		System.out.println("N:"+N+" M:"+M);
		maxN = N;
		maxM = M;
		a = val;
		makeStirlingTable();
	}

	public static int maxN() {
		return maxN;
	}

	public static int maxM() {
		return maxM;
	}

	/**
	 * The log Stirling number. N must be greater than or equal to M.
	 * 
	 * @param N
	 * @param M
	 * @return
	 */
	public static double logSN(int N, int M) {
		if (N > maxN || M > maxM) {
			if (N > maxN) {
				maxN = N + EXPSIZE;
			}
			if (M > maxM) {
				maxM = M + EXPSIZE;
			}
			stirlingTable.clear();
			makeStirlingTable();
		}

		return getValue(N, M);
	}

	/**
	 * The Stirling number.
	 * 
	 * @param N
	 * @param M
	 * @return
	 */
	public static double SN(int N, int M) {
		return Math.exp(logSN(N, M));
	}

	/**
	 * Save the stirling table
	 * 
	 * @param fileName
	 */
	public static void save(String fileName) {
		try {
			FileWriter writer = new FileWriter(fileName);
			for (int N = 1; N <= maxN; N++) {
				for (int M = 1; M <= maxM && M <= N; M++) {
					writer.write(String.format("%.3f ", getValue(N, M)));
				}
				writer.write("\n");
			}
			writer.close();
		} catch (IOException ioe) {
		}
	}

	/**
	 * Clear the memory allocated to the Stiriling table.
	 */
	public static void clear() {
		stirlingTable.clear();
	}

	public static void makeStirlingTable() {
		System.out.println("makeStirlingTable");
		int size = (maxM + 1) * (maxN + 1);
		stirlingTable = new Vector<Float>(size);
		for (int i = 0; i < size; i++)
			stirlingTable.add(i, Float.NaN);

		for (int N = 1; N < maxN; N++) {
			setValue(N, 0, Float.NEGATIVE_INFINITY);
			for (int M = N + 1; M < maxM; M++)
				setValue(N, M, Float.NEGATIVE_INFINITY);
		}
		setValue(0, 0, 0);
		setValue(1, 1, 0);
		double value = 0.0;
		for (int N = 2; N <= maxN; N++) {
			for (int M = 1; M <= maxM && M <= N; M++) {
				if (N == M) {
					setValue(N, M, 0);
				} else {
					value = logSum(getValue((N - 1), (M - 1)), (Math.log((N
							- (M * a) - 1.0)) + getValue((N - 1), M)));
					setValue(N, M, new Double(value).floatValue());
				}
				if (new Double(value).isInfinite()) {
					System.err.format(
							"S_NM(%d, %d) gone infinit during adding\n", N, M);
					System.exit(0);
				}
			}
		}

	}

	private static double logSum(double V, double lp) {
		if (lp > V) {
			double t = lp;
			lp = V;
			V = t;
		}
		return V + Math.log(1.0 + Math.exp(lp - V));
	}

	private static void setValue(int N, int M, float value) {
		int index = M * (maxN + 1) + N;
		stirlingTable.set(index, new Float(value));
	}

	private static float getValue(int N, int M) {
		int index = M * (maxN + 1) + N;
		return stirlingTable.get(index).floatValue();
	}
}
