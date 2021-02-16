package util;

import java.io.FileWriter;

/**
 * 
 * @author Lan Du
 *
 */
public class Matrix {
	/**
	 * Fill a double matrix with a given value.
	 * 
	 * @param matrix a double matrix
	 * @param val
	 */
	public static void fill(double[][] matrix, double val) {
		for (int i = 0; i < matrix.length; i++)
			for (int j = 0; j < matrix[i].length; j++)
				matrix[i][j] = val;
	}

	/**
	 * Copy a matrix from src to dest.
	 * 
	 * @param src
	 * @param dest
	 */
	public static void copy(double[][] src, double[][] dest) {
		assert src.length <= dest.length;
		assert src[0].length <= dest[0].length;
		for (int i = 0; i < src.length; i++)
			for (int j = 0; j < src[i].length; j++)
				dest[i][j] = src[i][j];
	}
	
	/**
	 * 
	 * @param v
	 */
	public static void print(double[][] v) {
		for (int i = 0; i < v.length; i++) {
			for (int j = 0; j < v[i].length; j++) {
				if (j == v[i].length - 1)
					System.out.printf("%.6f\n", v[i][j]);
				else
					System.out.printf("%.6f, ", v[i][j]);
			}
		}
	}
	
	/**
	 * 
	 * @param v
	 * @param file
	 */
	public static void write(double[][] v, String file) {
		try {
			FileWriter fw = new FileWriter(file);
			for (int i = 0; i < v.length; i++) {
				for (int j = 0; j < v[i].length; j++) {
					if (j == v[i].length - 1)
						fw.write(String.format("%.6f\n", v[i][j]));
					else
						fw.write(String.format("%.6f, ", v[i][j]));
				}
			}
			fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param v
	 */
	public static void print(int[][] v) {
		for (int i = 0; i < v.length; i++) {
			for (int j = 0; j < v[i].length; j++) {
				if (j == v[i].length - 1)
					System.out.printf("%d\n", v[i][j]);
				else
					System.out.printf("%d, ", v[i][j]);
			}
		}
	}

	public static void write(int[][] v, String file) {
		try {
			FileWriter fw = new FileWriter(file);
			for (int i = 0; i < v.length; i++) {
				for (int j = 0; j < v[i].length; j++) {
					if (j == v[i].length - 1)
						fw.write(String.format("%d\n", v[i][j]));
					else
						fw.write(String.format("%d, ", v[i][j]));
				}
			}
			fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
