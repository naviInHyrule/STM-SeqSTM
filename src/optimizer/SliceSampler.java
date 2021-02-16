package optimizer;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * 
 * The univariate slice sampler based on the "double" procedure, described in in
 * Neal (2003) "Slice Sampling", The Annals of Statistics 31(3), 705-767.
 * 
 * The follow java code is rewritten from C++ version by Mark Johnson
 * 
 * @author Lan Du
 * 
 */
public abstract class SliceSampler {
	private Object params;
	private RandomGenerator rand;

	private double min_x;
	private double max_x;

	private boolean doLogXTransform = false;

	public SliceSampler() {
		this.rand = new MersenneTwister();
	}

	public SliceSampler(RandomGenerator rand) {
		this.rand = rand;
	}

	/**
	 * Abstract function to implement the pdf.
	 * 
	 * @param x
	 * @param params
	 * @return
	 */
	public abstract double logpdf(double x, Object params);

	private double boundedDomainFun(double x) {
		if (min_x < x && x < max_x) {
			double fx = logpdf(x, params);
			if (Double.isInfinite(fx) || Double.isNaN(fx)) {
				System.err.println("logpdf(" + x + ")  = " + fx
						+ ", which is infinite or NaN");
				System.exit(1);
			}
			return fx;
		}
		return Double.NEGATIVE_INFINITY;
	}

	private double logDomainFun(double x) {
		assert !Double.isInfinite(x) && !Double.isNaN(x);
		double expx = Math.exp(x);
		assert !Double.isInfinite(expx) && !Double.isNaN(expx);
		double xlogFx = x + logpdf(expx, params);
		assert !Double.isInfinite(xlogFx) && !Double.isNaN(xlogFx);
		return xlogFx;
	}

	private double pdf(double x) {
		if (this.doLogXTransform)
			return logDomainFun(x);
		return boundedDomainFun(x);
	}

	public double sliceSample1D(Object params, double x0, double min_x,
			double max_x, double w, int nsamples, int ndoublings) {
		this.params = params;
		this.min_x = min_x;
		this.max_x = max_x;
		this.doLogXTransform = false;
		assert !Double.isInfinite(x0) && !Double.isNaN(x0);
		if (w <= 0.0) { // set w to a default width
			if (min_x > Double.NEGATIVE_INFINITY
					&& max_x < Double.POSITIVE_INFINITY)
				w = (max_x - min_x) / 4;
			else
				w = Math.max(((x0 < 0.0) ? -x0 : x0) / 2, 1e-7);
		}
		assert !Double.isInfinite(w) && !Double.isNaN(w);

		for (int sample = 0; sample < nsamples; sample++) {
			double x1 = stepping_out_sample(x0, w, ndoublings);
			assert !Double.isInfinite(x1) && !Double.isNaN(x1);
			w = 1.5 * (x1 - x0);
			if (w < 0)
				w = -w;
			x0 = x1;
		}
		return x0;
	}

	/**
	 * 
	 * @param params 
	 * @param y0 the starting point
	 * @param w the guess of the typical size of a slice
	 * @param nsamples number of samples to draw
	 * @param ndoublings number of doubling times
	 * @return the final sampled new state
	 */
	public double sliceSample1DP(Object params, double y0, double w,
			int nsamples, int ndoublings) {
		this.params = params;
		this.doLogXTransform = true;
		assert !Double.isInfinite(y0) && !Double.isNaN(y0);
		assert y0 > 0;
		assert w > 0;
		assert !Double.isInfinite(w) && !Double.isNaN(w);
		double x0 = Math.log(y0);

		for (int sample = 0; sample < nsamples; sample++) {
			double x1 = stepping_out_sample(x0, w, ndoublings);
			assert !Double.isInfinite(x1) && !Double.isNaN(x1);
			w = 2 * (x1 - x0);
			if (w < 0)
				w = -w;
			x0 = x1;
		}
		double fx0 = Math.exp(x0);
		assert !Double.isInfinite(fx0) && !Double.isNaN(fx0);
		return fx0;
	}

	/**
	 * Sample a new state with the "stepping-out" procedure.
	 * 
	 * @param x0
	 *            the current point
	 * @param w
	 *            estimate of the typical size of a slice
	 * @param m
	 *            integer limiting the size of a slice to mw
	 * @param params
	 * 
	 * @return a sampled new state
	 */
	private double stepping_out_sample(double x0, double w, int m) {
		double y = pdf(x0) + Math.log(rand.nextDouble() + 1e-100);
		DoublePair lr = new DoublePair();
		stepping_out(x0, y, w, m, lr);
		double x1 = shrinkage(x0, y, w, lr.l, lr.r, true);
		return x1;
	}

	/**
	 * Sample a new state with the "doubling" procedure.
	 * 
	 * @param x0
	 *            the current point
	 * @param w
	 *            estimate of the typical size of a slice
	 * @param p
	 *            integer limiting the size of a slice to 2^{p}w
	 * @param params
	 * @return a sample new state
	 */
	@SuppressWarnings("unused")
	private double doubling_sample(double x0, double w, int p) {
		double y = pdf(x0) + Math.log(rand.nextDouble() + 1e-100);
		DoublePair lr = new DoublePair();
		doubling(x0, y, w, p, lr);
		double x1 = shrinkage(x0, y, w, lr.l, lr.r, false);
		return x1;
	}

	/**
	 * The "stepping out" procedure for finding an interval around the curernt
	 * point. See Fig 5 in Neal 2003.
	 * 
	 * @param x0
	 *            the current point
	 * @param y
	 *            the vertical level defining the slice
	 * @param w
	 *            estimate of the typical size of slice
	 * @param m
	 *            integer limiting the size of a slice to mw
	 * @param lr
	 *            the left probe and the right probe
	 */
	private void stepping_out(double x0, double y, double w, int m,
			DoublePair lr) {
		double u = rand.nextDouble();
		lr.l = x0 - w * u;
		lr.r = lr.l + w;
		double v = rand.nextDouble();
		int j = (int) Math.floor(m * v);
		int k = (m - 1) - j;
		while (j > 0 && y < pdf(lr.l)) {
			lr.l -= w;
			j--;
		}
		while (k > 0 && y < pdf(lr.r)) {
			lr.r += w;
			k--;
		}
	}

	/**
	 * The "doubling" procedure for finding an interval around the current
	 * point. See Fig.4 in Neal 2003.
	 * 
	 * @param x0
	 *            the current point
	 * @param y
	 *            the vertical level defining the slice
	 * @param w
	 *            estimate of the typical size of a slice
	 * @param p
	 *            integer limiting the size of a slice to 2^{p}w
	 * @param lr
	 *            the left probe and the right probe
	 */
	private void doubling(double x0, double y, double w, int p, DoublePair lr) {
		double u = rand.nextDouble();
		lr.l = x0 - w * u;
		lr.r = lr.l + w;
		int k = p;
		while (k > 0 && (y < pdf(lr.l) || y < pdf(lr.r))) {
			double v = rand.nextDouble();
			if (v < 0.5)
				lr.l -= (lr.r - lr.l);
			else
				lr.r += (lr.r - lr.l);
			k--;
		}
	}

	/**
	 * The "shrinkage" procedure for sampling from the interval. See Fig.5 in
	 * Neal 2003.
	 * 
	 * @param x0
	 *            the current point
	 * @param y
	 *            the vertical level defining the slice
	 * @param w
	 *            estimate of the typical size of a slice
	 * @param l
	 *            the left probe
	 * @param r
	 *            the right probe
	 * @param always_accept
	 * @return
	 */
	private double shrinkage(double x0, double y, double w, double l, double r,
			boolean always_accept) {
		double lbar = l;
		double rbar = r;
		while (true) {
			double u = rand.nextDouble();
			double x1 = lbar + u * (rbar - lbar);
			double fx1 = pdf(x1);
			if (y < fx1 && (always_accept || acceptable(x0, x1, y, w, l, r)))
				return x1;
			if (x1 < x0)
				lbar = x1;
			else
				rbar = x1;
		}
	}

	/**
	 * The test for whether a new point, x1, that is an acceptable next state,
	 * whe the interval was found by the "doubling" procedure. See Fig.6 in Neal
	 * 2003.
	 * 
	 * @param x0
	 *            the current point
	 * @param x1
	 *            the possible next point
	 * @param y
	 *            the vertical level defining the slice
	 * @param w
	 *            estimate of the typical size of slice
	 * @param l
	 *            the left probe found by doubling procedure using w
	 * @param r
	 *            the right probe found by double procedure using w
	 * @return
	 */
	private boolean acceptable(double x0, double x1, double y, double w,
			double l, double r) {
		boolean d = false;
		double lbar = l;
		double rbar = r;

		while (rbar - lbar > 1.1 * w) {
			double m = (lbar + rbar) / 2.0;
			if ((x0 < m && x1 >= m) || (x0 >= m && x1 < m))
				d = true;
			if (x1 < m)
				rbar = m;
			else
				lbar = m;
			if (d && y >= pdf(lbar) && y >= pdf(rbar)) {
				return false;
			}
		}
		return true;
	}

	private class DoublePair {
		double l;
		double r;

		public DoublePair(double l, double r) {
			this.l = l;
			this.r = r;
		}

		public DoublePair() {
			this(0, 0);
		}
	}

}
