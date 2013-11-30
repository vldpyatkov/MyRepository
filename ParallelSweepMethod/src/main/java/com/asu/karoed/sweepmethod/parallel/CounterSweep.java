/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.asu.karoed.sweepmethod.parallel;

import com.asu.karoed.sweepmethod.interfaces.Sweep;
import com.asu.karoed.sweepmethod.sequential.LeftSweep;
import com.asu.karoed.sweepmethod.sequential.RightSweep;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author makros
 */
public class CounterSweep implements Sweep{
    
    private ExecutorService executor;
    
    //private ArrayList<ArrayList<Double>> rightSweepPart;
    ArrayList<Double> rightSweepA;
    ArrayList<Double> rightSweepC;
    ArrayList<Double> rightSweepB;
    private ArrayList<Double> rightSweepRPart;
    private ArrayList<Double> rightSweepResult;
    
    
    //private ArrayList<ArrayList<Double>> leftSweepPart;
    ArrayList<Double> leftSweepPartA;
    ArrayList<Double> leftSweepPartC;
    ArrayList<Double> leftSweepPartB;
    private ArrayList<Double> leftSweepRPart;
    private ArrayList<Double> leftSweepResult;
        
    private int dim;

    public CounterSweep(ArrayList<ArrayList<Double>> matrix, ArrayList<Double> rightPart) {
        
        this.executor = Executors.newFixedThreadPool(2);
        
        dim = matrix.size();
        int rDim = (dim%2>0)?dim/2 + 1:dim/2;
        int lDim = dim/2;
        
        rightSweepA = new ArrayList<>(rDim);
        rightSweepC = new ArrayList<>(rDim);
        rightSweepB = new ArrayList<>(rDim);
        rightSweepRPart = new ArrayList<>(rDim);
        fillSweepPart(rDim,0, matrix, rightPart, rightSweepA, rightSweepC, rightSweepB, rightSweepRPart);
        
        leftSweepPartA = new ArrayList<>(lDim);
        leftSweepPartC = new ArrayList<>(lDim);
        leftSweepPartB = new ArrayList<>(lDim);
        leftSweepRPart = new ArrayList<>(lDim);
        fillSweepPart(lDim, rDim, matrix, rightPart, leftSweepPartA, leftSweepPartC, leftSweepPartB, leftSweepRPart);
        
    }
    
    public CounterSweep(ArrayList<Double> matrixA, ArrayList<Double> matrixC,
            ArrayList<Double> matrixB, ArrayList<Double> rightPart) {

        this.executor = Executors.newFixedThreadPool(2);

        dim = matrixC.size();
        int rDim = (dim % 2 > 0) ? dim / 2 + 1 : dim / 2;
        int lDim = rDim + 1;

        rightSweepA = new ArrayList<>(rDim);
        rightSweepC = new ArrayList<>(rDim);
        rightSweepB = new ArrayList<>(rDim);
        rightSweepRPart = new ArrayList<>(rDim);
        fillSweepPart(matrixA, rightSweepA, 0, rDim);
        fillSweepPart(matrixC, rightSweepC, 0, rDim);
        fillSweepPart(matrixB, rightSweepB, 0, rDim);
        fillSweepPart(rightPart, rightSweepRPart, 0, rDim);
        //fillSweepPart(rDim, 0, matrix, rightPart, rightSweepA, rightSweepC, rightSweepB, rightSweepRPart);

        leftSweepPartA = new ArrayList<>(lDim);
        leftSweepPartC = new ArrayList<>(lDim);
        leftSweepPartB = new ArrayList<>(lDim);
        leftSweepRPart = new ArrayList<>(lDim);
        fillSweepPart(matrixA, leftSweepPartA, rDim-1, lDim);
        fillSweepPart(matrixC, leftSweepPartC, rDim-1, lDim);
        fillSweepPart(matrixB, leftSweepPartB, rDim-1, lDim);
        fillSweepPart(rightPart, leftSweepRPart, rDim-1, lDim);
        //fillSweepPart(lDim, rDim, matrix, rightPart, leftSweepPartA, leftSweepPartC, leftSweepPartB, leftSweepRPart);

    }
    
    private void fillSweepPart(ArrayList<Double> source, ArrayList<Double> part, int start, int count) {
        for (int i = start; i<start+count; i++) {
            part.add(source.get(i));
        }        
    }

    @Override
    public ArrayList<Double> getResult() {
        ArrayList<Double> result = new ArrayList<>(rightSweepResult);
        for (int i=1; i<leftSweepResult.size(); i++){
            result.add(leftSweepResult.get(i));
        }
        return result;
    }

    @Override
    public void solve() {
        final CountDownLatch latch = new CountDownLatch(2);
        final double[] rightCoof = new double[2];
        final double[] leftCoof = new double[2];
        try {
            Callable<ArrayList<Double>> rightSweep = new Callable<ArrayList<Double>>() {

                @Override
                public ArrayList<Double> call() throws Exception {
                    Sweep rightSweep = new RightSweep(rightSweepA, rightSweepC,
                            rightSweepB, rightSweepRPart) {
                        @Override
                        protected double startSweepVar() {
                            int rDim = (dim % 2 > 0) ? dim / 2 + 1 : dim / 2;                            
                            
                            double den = (rightSweepA.get(rDim-1)*alpha[rDim-1] + rightSweepC.get(rDim-1));
                            rightCoof[0] = -rightSweepB.get(rDim - 1) / den;
                            rightCoof[1] = (rightSweepRPart.get(rDim - 1) - rightSweepA.get(rDim - 1) * beta[rDim - 1]) / den;
                            latch.countDown();
                            try {
                                latch.await();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(CounterSweep.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            double x = (rightCoof[0]*leftCoof[1] + rightCoof[1])
                                    /(1 - rightCoof[0]*leftCoof[0]);
                            return x;
                        }
                    };
                    rightSweep.solve();
                    return rightSweep.getResult();
                }
            };
            
            Callable<ArrayList<Double>> leftSweep = new Callable<ArrayList<Double>>() {

                @Override
                public ArrayList<Double> call() throws Exception {
                    Sweep leftSweep = new LeftSweep(leftSweepPartA, leftSweepPartC,
                            leftSweepPartB, leftSweepRPart) {                        
                        @Override
                        protected double startSweepVar() {
                            //int lDim = dim / 2;
                            
//                            double den = leftSweepPartC.get(0) + leftSweepPartB.get(0) * ksi[1];
////
//                            leftCoof[0] = -leftSweepPartA.get(0) / den;
//                            leftCoof[1] = (leftSweepRPart.get(0) - leftSweepPartB.get(0) * eta[1]) / den;
                            leftCoof[0] = ksi[1];
                            leftCoof[1] = eta[1];
                            latch.countDown();
                            try {
                                latch.await();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(CounterSweep.class.getName()).log(Level.SEVERE, null, ex);
                            }
//                            double x = (ksi[1]*rightCoof[1] + eta[1])
//                                    /(1 - ksi[1]*rightCoof[0]);
                            double x = (rightCoof[0]*leftCoof[1] + rightCoof[1])
                                    /(1 - rightCoof[0]*leftCoof[0]);
                            return x;
                        }                        
                    };
                    leftSweep.solve();
                    return leftSweep.getResult();
                }
            };
            
            ArrayList<Callable<ArrayList<Double>>> callables = new ArrayList<>();
            callables.add(rightSweep);
            callables.add(leftSweep);
            List<Future<ArrayList<Double>>> futures = executor.invokeAll(callables);
            rightSweepResult = futures.get(0).get();
            leftSweepResult = futures.get(1).get();

        }
        catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(CounterSweep.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
    }

    private void fillSweepPart(int partDim, int startIndex,
            ArrayList<ArrayList<Double>> matrix, ArrayList<Double> rightPart,
            ArrayList<Double> partA, ArrayList<Double> partC,
            ArrayList<Double> partB, ArrayList<Double> rPart) {
        partA.add(0.);
        for (int i=0; i<partDim; i++) {
            //part.add(new ArrayList<Double>(partDim));
            for (int j=0; j<partDim; j++) {
                //part.get(i).add(matrix.get(startIndex + i).get(startIndex + j));
                if (j - 1 == i) {
                    partA.add(matrix.get(i).get(j));
                } else if (j == i) {
                    partC.add(matrix.get(i).get(j));
                } else if (j + 1 == i) {
                    partB.add(matrix.get(i).get(j));
                }
            }
            rPart.add(rightPart.get(startIndex + i));
        }
        partB.add(0.);
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }
    
}
