package util;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.special.Beta;
import org.apache.commons.math3.special.Gamma;

/**
 * 
 * @author Lan Du
 *
 */
public class SpecialFuns {
	/**
	 * Return the value of the log pochhammer Symbol (x|y)_n
	 * 
	 * @param x
	 * @param y
	 * @param n
	 * @return
	 */
	public static double logPochSym(double x, double y, double n) {
		double logValue;
		if (y == 0.0) {
			logValue = n * Math.log(x);
		} else {
			double tmp = x / y;
			logValue = Gamma.logGamma(tmp + n) - Gamma.logGamma(tmp) + n
					* Math.log(y);
		}
		return logValue;
	}

	/**
	 * Return the value of the pochhammer Symbol (x|y)_n
	 * 
	 * @param x
	 * @param y
	 * @param n
	 * @return
	 */
	public static double pochSym(double x, double y, double n) {
		return Math.exp(logPochSym(x, y, n));
	}

	/**
	 * 
	 * @param x
	 * @return
	 */
	public static long convertToUnsignedInt(int x) {
		return x & 0xFFFFFFFFL;
	}

	/**
	 * The log gamma function
	 * 
	 * @param x
	 * @return
	 */
	public static double logGamma(double x) {
		return Gamma.logGamma(x);
	}

	/**
	 * The gamma function
	 * 
	 * @param x
	 * @return
	 */
	public static double Gamma(double x) {
		return Gamma.gamma(x);
	}

	/*
	 * Beta function
	 */
	public static double logBeta(double x, double y) {
		return Beta.logBeta(x, y);
	}

	/**
	 * The log choose function
	 * 
	 * @param n
	 * @param m
	 * @return
	 */
	public static double logChoose(int n, int m) {
		return ArithmeticUtils.binomialCoefficientLog(n, m);
	}

	/**
	 * The choose fuction
	 * 
	 * @param n
	 * @param m
	 * @return
	 */
	public static double choose(int n, int m) {
		return ArithmeticUtils.binomialCoefficientDouble(n, m);
	}

	/**
	 * The fuction used to test the Double value.
	 * 
	 * @param val
	 * @return
	 */
	public static boolean isnormal(double val) {
		return !Double.isInfinite(val) && !Double.isNaN(val);
	}
}
