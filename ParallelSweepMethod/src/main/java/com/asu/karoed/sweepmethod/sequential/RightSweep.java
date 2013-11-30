/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.asu.karoed.sweepmethod.sequential;

import com.asu.karoed.sweepmethod.interfaces.Sweep;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Karoed
 * 
 * c b . . .
 * a c b . .
 * . a c b .
 * .........
 * . . a c b
 * . . . a c
 */
public class RightSweep implements Sweep {
    
    protected double beta[];
    protected double alpha[];
    //ArrayList<ArrayList<Double>> matrix;
    ArrayList<Double> a;
    ArrayList<Double> c;
    ArrayList<Double> b;
    
    
    ArrayList<Double> rightPart;
    double[] result;
    int dim;

    public RightSweep(ArrayList<ArrayList<Double>> matrix, ArrayList<Double> rightPart) {
        //this.matrix = matrix;
        this.rightPart = rightPart;
        this.dim = matrix.size();
        this.beta = new double[dim];
        this.alpha = new double[dim];
        this.result = new double[dim];
        initFromMatrix(matrix);        
    }
    
    public RightSweep(ArrayList<Double> a, ArrayList<Double> c,
            ArrayList<Double> b, ArrayList<Double> rightPart) {
        //this.matrix = matrix;
        this.rightPart = rightPart;
        this.dim = b.size();
        this.beta = new double[dim];
        this.alpha = new double[dim];
        this.result = new double[dim];
        this.a = a;
        this.c = c;
        this.b = b;
    }
    
    private void calculateNextCoeff(int level) {
        
        double den = (a.get(level-1)*alpha[level-1] + c.get(level-1));
        
        alpha[level] = - b.get(level-1)/den;
        beta[level] = (rightPart.get(level-1) - a.get(level-1)*beta[level-1])/den;
        
    }
    
    private void calculateNextVar(int level) {
        result[level] = alpha[level+1]*result[level+1] + beta[level+1];
    }
    
    @Override
    public void solve() {        
        startSweepCoeff();
        for (int i=2; i<dim; i++) {
            calculateNextCoeff(i);
        }
        
        result[dim-1] = startSweepVar();
        for (int i=dim-2; i>=0; i--) {
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
        alpha[1] = - b.get(0)/c.get(0);
        beta[1] = rightPart.get(0)/c.get(0);
    }

    protected double startSweepVar() {
        return (rightPart.get(dim-1)- a.get(dim-1)*beta[dim-1])/
                (a.get(dim-1)*alpha[dim-1] + c.get(dim-1));
    }

    @Override
    public void close() throws IOException {
        
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
    
}
