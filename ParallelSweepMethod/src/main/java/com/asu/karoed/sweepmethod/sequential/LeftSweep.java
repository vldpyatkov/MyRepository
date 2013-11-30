/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.asu.karoed.sweepmethod.sequential;

import com.asu.karoed.sweepmethod.interfaces.Sweep;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Karoed
 * 
 * c b . . .
 * a c b . .
 * . a c b .
 * .........
 * . . a c b
 * . . . a c
 */
public class LeftSweep implements Sweep{
    
    protected double eta[];
    protected double ksi[];
    //ArrayList<ArrayList<Double>> matrix;
    ArrayList<Double> a;
    ArrayList<Double> c;
    ArrayList<Double> b;
    ArrayList<Double> rightPart;
    double[] result;
    int dim;

    public LeftSweep(ArrayList<ArrayList<Double>> matrix, ArrayList<Double> rightPart) {
        this.rightPart = rightPart;
        this.dim = matrix.size();
        this.eta = new double[dim];
        this.ksi = new double[dim];
        this.result = new double[dim];
        initFromMatrix(matrix);
    }
    
    public LeftSweep(ArrayList<Double> a, ArrayList<Double> c,
        ArrayList<Double> b, ArrayList<Double> rightPart) {
        //this.matrix = matrix;
        this.rightPart = rightPart;
        this.dim = b.size();
        this.ksi = new double[dim];
        this.eta = new double[dim];
        this.result = new double[dim];
        this.a = a;
        this.c = c;
        this.b = b;
    }
    
    private void initFromMatrix(ArrayList<ArrayList<Double>> matrix) {
        a = new ArrayList<>(dim);
        c = new ArrayList<>(dim);
        b = new ArrayList<>(dim);

        int rowIndex = 0;
        int columnIndex = 0;
        a.add(0.);
        for (ArrayList<Double> row : matrix) {
            for (Double element : row) {
                if (columnIndex - 1 == rowIndex) {
                    a.add(element);
                } else if (columnIndex == rowIndex) {
                    c.add(element);
                } else if (columnIndex + 1 == rowIndex) {
                    b.add(element);
                }
                columnIndex++;
            }
            rowIndex++;
            columnIndex = 0;
        }
        b.add(0.);
    }
    
    private void calculateNextCoeff(int level) {
        
        double den = c.get(level) + b.get(level)*ksi[level+1];
        
        ksi[level] = - a.get(level)/den;
        eta[level] = (rightPart.get(level) - b.get(level)*eta[level+1])/den;        
    }
    
    private void calculateNextVar(int level) {
        result[level] = ksi[level]*result[level-1] + eta[level];
    }
    
    @Override
    public void solve() {        
        startSweepCoeff();
        for (int i=dim-2; i>0; i--) {
            calculateNextCoeff(i);
        }
        
        result[0] = startSweepVar();
        for (int i=1; i<dim; i++) {
            calculateNextVar(i);
        }
    }
    
    @Override
    public ArrayList<Double> getResult() {
        ArrayList<Double> arr = new ArrayList<>(dim);
        for (int i=0; i<dim; i++) {
            arr.add(result[i]);
        }
        return arr;
    }

    protected void startSweepCoeff() {
        ksi[dim-1] = - a.get(dim-1)/c.get(dim-1);
        eta[dim-1] = rightPart.get(dim-1)/c.get(dim-1);
    }

    protected double startSweepVar() {
        return (rightPart.get(0) - b.get(0)*eta[1])/
                (b.get(0)*ksi[1] + c.get(0));
    }

    @Override
    public void close() throws IOException {
        
    }
    
}
