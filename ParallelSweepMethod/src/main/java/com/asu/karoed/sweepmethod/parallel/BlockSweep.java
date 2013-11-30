package com.asu.karoed.sweepmethod.parallel;

import com.asu.karoed.sweepmethod.interfaces.Sweep;
import com.asu.karoed.sweepmethod.sequential.RightSweep;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: makros
 * Date: 5/4/13
 * Time: 1:14 AM
 * a c . . .|d
 * b a c . .|d
 * . b a c .|d
 * .........|d
 * . . b a c|d
 * . . . b a|d
 */
public class BlockSweep implements Sweep {

    private ExecutorService executor;
    final CountDownLatch latch;
    final CyclicBarrier barrier;

    final ConcurrentHashMap<Integer, Double> results;

    final ConcurrentHashMap<Integer, Double[]> lastEquations;
    final ConcurrentHashMap<Integer, Double[]> lastSquenceEquations;

    final ArrayList<Double> a;
    final ArrayList<Double> b;
    final ArrayList<Double> c;
    final ArrayList<Double> d;

    ArrayList<Callable<Double[]>> callables;

    public BlockSweep(ArrayList<Double> b, ArrayList<Double> a, ArrayList<Double> c, ArrayList<Double> d, final int threads) {
        final int fullDim = a.size();
        if (fullDim % threads != 0) {
            throw new IllegalArgumentException("Matrix dimension must be multiple number of streams");
        }
        final int subSystemDim = fullDim/threads;
        results = new ConcurrentHashMap<>(fullDim);

        executor = Executors.newFixedThreadPool(threads);
        latch = new CountDownLatch(threads);
        barrier = new CyclicBarrier(threads, new Runnable() {
            @Override
            public void run() {
                //
                ArrayList<Double> aPart = new ArrayList<>(threads);
                ArrayList<Double> bPart = new ArrayList<>(threads);
                ArrayList<Double> cPart = new ArrayList<>(threads);
                ArrayList<Double> dPart = new ArrayList<>(threads);
                for (int i=0; i<threads; i++) {
                    aPart.add(lastSquenceEquations.get(i)[0]);
                    bPart.add(lastSquenceEquations.get(i)[1]);
                    cPart.add(lastSquenceEquations.get(i)[2]);
                    dPart.add(lastSquenceEquations.get(i)[3]);
                }
                RightSweep rightSweep = new RightSweep(bPart, aPart, cPart, dPart);
                rightSweep.solve();
                ArrayList<Double> partResult = rightSweep.getResult();
                for (int i=0; i<threads; i++) {
                    results.put((i+1)*subSystemDim -1, partResult.get(i));
                }
            }
        });
        lastEquations = new ConcurrentHashMap<>(threads);
        lastSquenceEquations = new ConcurrentHashMap<>(threads);

        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;

        callables = new ArrayList<>(threads);

        for (int i=0; i<threads; i++) {
            final int partNo = i;
            callables.add(new Callable<Double[]>() {
                @Override
                public Double[] call() throws Exception {
                    int start = partNo*subSystemDim;
                    ArrayList<Double> aPart = subArray(BlockSweep.this.a, start, subSystemDim);
                    ArrayList<Double> bPart = subArray(BlockSweep.this.b, start, subSystemDim);
                    ArrayList<Double> cPart = subArray(BlockSweep.this.c, start, subSystemDim);
                    ArrayList<Double> dPart = subArray(BlockSweep.this.d, start, subSystemDim);

                    FirstPass firstPass = new FirstPass(aPart,bPart,cPart,dPart);
                    firstPass.pass();
                    SecondPass secondPass = new SecondPass(firstPass.aPart, firstPass.bPart,
                            firstPass.cPart, firstPass.dPart);
                    secondPass.pass();
                    lastEquations.put(partNo, new Double[] {secondPass.aPart.get(0),
                            secondPass.bPart.get(0), secondPass.cPart.get(0),
                            secondPass.dPart.get(0)});
                    latch.countDown();
                    try {
                        latch.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(CounterSweep.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Double[] eq;
                    if (partNo+1 < lastEquations.size()) {
                        Double[] nextPartLastEq = lastEquations.get(partNo+1);
                        eq = secondPass.lastEquation(nextPartLastEq[0], nextPartLastEq[1],
                                nextPartLastEq[2], nextPartLastEq[3]);
                    }
                    else {
                        int lastIndex = subSystemDim - 1;
                        eq = new Double[] {secondPass.aPart.get(lastIndex), secondPass.bPart.get(lastIndex),
                                secondPass.cPart.get(lastIndex), secondPass.dPart.get(lastIndex)};
                    }
                    lastSquenceEquations.put(partNo, eq);
                    barrier.await();
                    for (int i = subSystemDim-2; i>=0; i--) {
                        results.put(start+i, (secondPass.dPart.get(i) - secondPass.cPart.get(i)*results.get(start+subSystemDim-1)-
                                ((start>0) ? secondPass.bPart.get(i)*results.get(start-1) : 0))/secondPass.aPart.get(i));
                    }
                    //
                    return new Double[]{};
                }
            });
        }

    }

    private ArrayList<Double> subArray(ArrayList<Double> source, int start, int length) {
        ArrayList<Double> result = new ArrayList<>(length);
        for (int i=0; i<length; i++) {
            result.add(source.get(start+i));
        }
        return result;
    }

    private class FirstPass {
        ArrayList<Double> aPart;
        ArrayList<Double> bPart;
        ArrayList<Double> cPart;
        ArrayList<Double> dPart;

        FirstPass(ArrayList<Double> a, ArrayList<Double> b, ArrayList<Double> c, ArrayList<Double> d) {
            aPart = a;
            bPart = b;
            cPart = c;
            dPart = d;
        }

        void pass() {
            int dim = aPart.size();
            for (int i=1; i<dim; i++) {
                double mult = -bPart.get(i) / aPart.get(i-1);
                aPart.set(i, aPart.get(i) + mult * cPart.get(i-1));
                bPart.set(i, mult * bPart.get(i-1));
                dPart.set(i, dPart.get(i) + mult * dPart.get(i-1));
            }
        }

    }

    private class SecondPass {

        ArrayList<Double> aPart;
        ArrayList<Double> bPart;
        ArrayList<Double> cPart;
        ArrayList<Double> dPart;

        SecondPass(ArrayList<Double> a, ArrayList<Double> b, ArrayList<Double> c, ArrayList<Double> d) {
            aPart = a;
            bPart = b;
            cPart = c;
            dPart = d;
        }

        void pass() {
            int dim = aPart.size();
            for (int i=dim-3; i>=0; i--) {
                double mult = -cPart.get(i)/aPart.get(i+1);
                bPart.set(i, bPart.get(i) + mult*bPart.get(i+1));
                cPart.set(i, mult*cPart.get(i+1));
                dPart.set(i, dPart.get(i) + mult*dPart.get(i+1));
            }
        }

        Double[] lastEquation(double a1, double b1, double c1, double d1) {
            int lastIndex = aPart.size()-1;
            double mult = -cPart.get(lastIndex)/a1;
            aPart.set(lastIndex, aPart.get(lastIndex) + mult*b1);
            cPart.set(lastIndex, mult*c1);
            dPart.set(lastIndex, dPart.get(lastIndex) + mult*d1);

            Double[] result = new Double[4];
            result[0] = aPart.get(lastIndex);
            result[1] = bPart.get(lastIndex);
            result[2] = cPart.get(lastIndex);
            result[3] = dPart.get(lastIndex);

            return result;
        }

    }


    @Override
    public ArrayList<Double> getResult() {
        ArrayList<Double> res = new ArrayList<>(results.size());
        SortedSet<Integer> keys = new TreeSet<>(results.keySet());
        for (Integer key : keys) {
            res.add(results.get(key));
        }
        return res;
    }

    @Override
    public void solve() {
        try {
            executor.invokeAll(callables);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }
}
