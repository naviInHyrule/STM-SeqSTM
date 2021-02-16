package util;

import java.io.FileWriter;

import org.apache.commons.math3.stat.StatUtils;

/**
 * 
 * @author Lan Du
 *
 */
public class Vector {
	/**
	 * The maximum value in a double array.
	 * 
	 * @param values
	 * @return
	 */
	public static double max(double[] values) {
		return StatUtils.max(values);
	}
	
	/**
	 * The sum of a double array
	 * 
	 * @param values
	 * @return
	 */
	public static double sum(double[] values) {
		return StatUtils.sum(values);
	}
	
	/**
	 * return the mean value of a double vector.
	 * @param values
	 * @return
	 */
	public static double mean(double[] values) {
		return StatUtils.mean(values);
	}
	
	/**
	 * 
	 * @param v
	 * @return
	 */
	public static String toString(double[] v) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < v.length; i++) {
			buf.append(String.format("%.6f", v[i]));
			if (i < v.length - 1)
				buf.append(", ");
		}
		return buf.toString();
	}
	
	/**
	 * Print a double vector in console.
	 * 
	 * @param v
	 */
	public static void print(double[] v) {
		System.out.printf("[");
		for (int i = 0; i < v.length; i++) {
			if (i == v.length - 1)
				System.out.printf("%.6f", v[i]);
			else
				System.out.printf("%.6f, ", v[i]);
		}
		System.out.printf("]\n");
	}
	
	/**
	 * Write a double vector to a file.
	 * 
	 * @param v
	 * @param file
	 */
	public static void write(double[] v, String file) {
		try {
			FileWriter fw = new FileWriter(file);
			for (int i = 0; i < v.length; i++)
				fw.write(String.format("%.6f\n", v[i]));
			fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Print an int vector in console.
	 * 
	 * @param v
	 */
	public static void print(int[] v) {
		System.out.printf("[");
		for (int i = 0; i < v.length; i++) {
			if (i == v.length - 1)
				System.out.printf("%d", v[i]);
			else
				System.out.printf("%d, ", v[i]);
		}
		System.out.printf("]\n");
	}

	/**
	 * 
	 * @param v
	 * @return
	 */
	public static String toString(int[] v) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < v.length; i++) {
			if (i == v.length - 1)
				buf.append(String.format("%d", v[i]));
			else
				buf.append(String.format("%d, ", v[i]));
		}
		return buf.toString();
	}
}
