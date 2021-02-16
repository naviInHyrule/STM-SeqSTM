package model;

import data.Corpus;
import data.Segment;
import states.GibbsConfigs;
import states.Parameters;
import states.StatsTables;
import states.TopicDists;
import util.*;

/**
 *
 * @author Mariflor Vega
 *
 */
public class PerplexitySampler {
    private Corpus corpus;
    private StatsTables stables;
    private Parameters modelParams;
    private GibbsConfigs configs;
    //Serialization
    private static final long serialVersionUID = 1L;
    //Class variables
    private double[] indOneProbs, topicProbs;
    //cached value
    private static double[][] stirRatioOne;
    private static double[][] stirRatioTwo;

    public PerplexitySampler (Corpus corpus,
                             Parameters modelParams, GibbsConfigs configs) {

        this.corpus=corpus;
        this.modelParams=modelParams;
        this.configs=configs;
        indOneProbs = new double[modelParams.numTopics()];
        stables = new StatsTables(modelParams.numTopics(), modelParams.numTypes(), corpus);
        topicProbs = new double[modelParams.numTopics()];
        if(Parameters.debug)
            System.out.println("\nRun indicator sampling algorithm!!!\n");
    }

    /**
     * Initialise all the cached values.
     */
    public void initCachedValues() {
        stirRatioOne = new double[StirNum.maxM()][StirNum.maxN()];
        Matrix.fill(stirRatioOne, Double.NaN);

        stirRatioTwo = new double[StirNum.maxM()][StirNum.maxN()];
        Matrix.fill(stirRatioTwo, Double.NaN);

        stirRatioOne[0][0] =  Math.exp(StirNum.logSN(1, 1) - StirNum.logSN(0, 0));
        for(int t = 1; t < stirRatioOne.length; t++) {
            for(int n = t; n < stirRatioOne[t].length; n++){
                stirRatioOne[t][n] = Math.exp(StirNum.logSN(n+1, t+1) - StirNum.logSN(n, t))
                        *(t + 1.0)/(n + 1.0);
                stirRatioTwo[t][n] = Math.exp(StirNum.logSN(n+1, t) - StirNum.logSN(n, t))
                        *(n - t + 1.0)/(n + 1.0);
            }
        }
    }
    /**
     * Run LefttoRight Perplexity, R number of particles
     */
    public double[][] runPerplexity(int R) {
        int w=0;
        double pn;
        int D =corpus.numDocs();
        int K =modelParams.numTopics();
        double[][] logllk= new double[D][];
        int Ns;
        for (int i = 0; i < D; i++) {
            System.out.println("D"+i+"-----------------------------------------");
            int Nd=corpus.getDoc(i).numSegs();
            logllk[i]= new double[Nd];
            for (int j = 0; j < Nd; j++) {
                Segment tp = corpus.getDoc(i).getSegment(j);
                pn = 0.0;
                Ns=tp.size();
                for (int n = 0; n < Ns; n++) {

                    for(int r=0; r<R; r++) {
                        stables.clean(i,j);
                        for (int n_ = 0; n_ < n; n_++)
                            w= tp.getWord(n_);
                            sample(i, j, w);

                        for (int k = 0; k < K; k++) {
                            w= tp.getWord(n);
                            pn += modelParams.getPhi(k, w) *
                                    ((stables.NIJK[i][j][k] - modelParams.geta()
                                            * stables.TIJK[i][j][k])
                                            + (modelParams.geta() * stables.TIJ[i][j]
                                            + modelParams.getb(i))
                                            * modelParams.getTheta(i, k)) / (modelParams.getb(i) + stables.NIJ[i][j]);

                            //System.out.println("phiw: "+Phi.getScore(k, n)+" alphaSum: "+alphaSum+ " alpha: "+alpha+" NS: "+sum(Ns)+" pn:"+pn);
                        }

                    }
                    pn /= R;
                    logllk[i][j] += Math.log(pn);
                }
            }
        }
        return(logllk);
    }
    /**
     * Get a cached value.
     *
     * @param n
     *            the number of customers
     * @param t
     *            the number of tables
     * @return
     */
    private double getStirRatioOne(final int n, final int t){
        if(t >= stirRatioOne.length || n >= stirRatioOne[0].length){
            double[][] tmp = stirRatioOne;
            int sizeM = tmp.length;
            int sizeN = tmp[0].length;
            if(t > sizeM)
                sizeM += StirNum.EXPSIZE;
            if(n > sizeN)
                sizeN += StirNum.EXPSIZE;
            stirRatioOne = new double[sizeM][sizeN];
            Matrix.fill(stirRatioOne, Double.NaN);
            Matrix.copy(tmp, stirRatioOne);
        }
        if(stirRatioOne[t][n] == Double.NaN)
            stirRatioOne[t][n] = Math.exp(StirNum.logSN(n+1, t+1) - StirNum.logSN(n, t))
                    *(t + 1.0)/(n + 1.0);
        return stirRatioOne[t][n];
    }

    /**
     * Get a cached value.
     * @param n
     * @param t
     * @return
     */
    private double getStirRatioTwo (final int n, final int t){
        if(t >= stirRatioTwo.length || n >= stirRatioTwo[0].length){
            double[][] tmp = stirRatioTwo;
            int sizeM = tmp.length;
            int sizeN = tmp[0].length;
            if(t > sizeM)
                sizeM += StirNum.EXPSIZE;
            if(n > sizeN)
                sizeN += StirNum.EXPSIZE;
            stirRatioTwo = new double[sizeM][sizeN];
            Matrix.fill(stirRatioTwo, Double.NaN);
            Matrix.copy(tmp, stirRatioTwo);
        }
        if(stirRatioTwo[t][n] == Double.NaN)
            stirRatioTwo[t][n] = Math.exp(StirNum.logSN(n+1, t) - StirNum.logSN(n, t))
                    *(n - t + 1.0)/(n + 1.0);
        return stirRatioTwo[t][n];
    }

    /**
     * Jointly sample topic and table indicator, and
     * return the sample topic.
     *
     * @param i
     * @param j
     * @param w
     * @return
     */
    protected int sample(final int i, final int j, final int w){
        for(int k = 0; k < modelParams.numTopics(); k++){

            double phi = modelParams.getPhi(k, w);
            double theta = modelParams.getTheta(i,k);

            indOneProbs[k] = probIndOne(i, j, k, phi, theta);

            topicProbs[k] = indOneProbs[k];
            if(stables.TIJK[i][j][k] > 0)
                topicProbs[k] += probIndZero(i, j, k, phi);
        }
        int z = this.nextDiscrete(topicProbs);
        double indProb = indOneProbs[z] / topicProbs[z];
        if (indProb > MTRandom.nextDouble())
            stables.adjustTable(i, j, z, 1);
        stables.adjustCust(i, j, w, z, 1);

        //System.out.println("sample k="+k);
        return z;
    }


    /**
     * Return the joint probablity of increasing both NIJK and TIJK
     * by one.
     * @param i document index
     * @param j text passage index
     * @param k topic index
     * @return
     *

     */

    private double probIndOne(final int i, final int j,
                              final int k, double val, double theta)
    {
        val *= this.getStirRatioOne(stables.NIJK[i][j][k], stables.TIJK[i][j][k])
                *theta
                *this.Pochratio(i,j);
        if(!SpecialFuns.isnormal(val) || val < 0)
            throw new RuntimeException("Illegal probability of ind = 1!!!");
        return val;
    }


    private double Pochratio(final int i,  final int j)
    {
        double val = (modelParams.getb(i) + modelParams.geta()*stables.TIJ[i][j]) /
                (modelParams.getb(i) + stables.NIJ[i][j]);

        if(!SpecialFuns.isnormal(val) || val < 0)
            throw new RuntimeException("Illegal probability of ind = 1!!!");
        return val;
    }


    /**
     * Return the joint probability of increaing NIJK by one
     * and keeping TIJK unchanged.
     * @param i document index
     * @param j text passage index
     * @param k topic index
     * @param val pre-computed phi value
     * @return
     */
    public double probIndZero(final int i, final int j,
                              final int k, double val)
    {
        val *= this.getStirRatioTwo(stables.NIJK[i][j][k], stables.TIJK[i][j][k])
                / (modelParams.getb(i) + stables.NIJ[i][j]);
        if(!SpecialFuns.isnormal(val) || val < 0)
            throw new RuntimeException("Illegal probability of ind = 0!!!");
        return val;
    }

    /**
     * Sample a value from a double array;
     *
     * @param probs an double array
     * @return
     */
    public int nextDiscrete(double[] probs) {
        double sum = 0.0;
        double r = MTRandom.nextDouble() * Vector.sum(probs);
        for (int i = 0; i < probs.length; i++) {
            sum += probs[i];
            if (sum > r) return i;
        }
        return probs.length - 1;
    }


}
