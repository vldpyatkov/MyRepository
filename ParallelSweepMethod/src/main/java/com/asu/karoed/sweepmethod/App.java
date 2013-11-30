package com.asu.karoed.sweepmethod;

import com.asu.karoed.sweepmethod.interfaces.Sweep;
import com.asu.karoed.sweepmethod.parallel.BlockSweep;
import com.asu.karoed.sweepmethod.parallel.CounterSweep;
import com.asu.karoed.sweepmethod.sequential.LeftSweep;
import com.asu.karoed.sweepmethod.sequential.RightSweep;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.ArrayList;

/**
 * @author Karoed
 * 
 * c b . . .|f
 * a c b . .|f
 * . a c b .|f
 * .........|f
 * . . a c b|f
 * . . . a c|f
 *
 */
public class App 
{
    public static void printArr(ArrayList<Double> arr) {
        for (Double d : arr) {
            System.out.print(d + " ");
        }
        System.out.println();
    }
    
    
    public static void main( String[] args ) throws FileNotFoundException, IOException {
        //ArrayList<ArrayList<Double>> matrix = null;
        ArrayList<Double> a = new ArrayList<>();
        ArrayList<Double> c = new ArrayList<>();
        ArrayList<Double> b = new ArrayList<>();        
        ArrayList<Double> row = null;
        try (FileReader fileReader = new FileReader(
                "..\\ThreeDiagonalMatrigGenerator\\OutMatrix.txt")) {
        
            StreamTokenizer streamTokenizer = new StreamTokenizer(fileReader);
            streamTokenizer.whitespaceChars(',', ',');
            streamTokenizer.whitespaceChars('\n', '\n');
            streamTokenizer.eolIsSignificant(true);

            int tokenType;
            int iterations = 0;
            row = new ArrayList<>();
            int rowIndex = 0;
            int columnIndex = 0;
            boolean lastLine = false;
            a.add(0.);
            while ((tokenType = streamTokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
                iterations++;
                if (tokenType == StreamTokenizer.TT_NUMBER) {
                    if (lastLine) {
                        row.add(streamTokenizer.nval);
                    }
                    else {
                        if (columnIndex+1 == rowIndex) {
                            a.add(streamTokenizer.nval);
                        }
                        else if (columnIndex == rowIndex) {
                            c.add(streamTokenizer.nval);
                        }
                        else if (columnIndex-1 == rowIndex) {
                            b.add(streamTokenizer.nval);
                        }                        
                    }
                    columnIndex ++;
                    //System.out.print(String.format("%1$.3f \t", streamTokenizer.nval));
                }
                else if (tokenType == StreamTokenizer.TT_EOL) {
                    if (columnIndex-1 == rowIndex) {
                        lastLine = true;
                    }
                    columnIndex = 0;
                    rowIndex++;
                    //System.out.println();
                }
            }
            b.add(0.);
            System.out.print("a : ");
            printArr(a);
            System.out.print("b : ");
            printArr(b);
            System.out.print("c : ");
            printArr(c);
            System.out.print("f : ");
            printArr(row);
            
            System.out.println( "\nIterations = " + iterations );
        }
        long[] fullTime = new long[4];
        for (int i=0; i<fullTime.length; i++) {
            fullTime[i]=0;
        }
         
        int count = 10;
        long currentTime;
        long now;
        ArrayList<Double> vars = null;
        //Правая прогонка
        Sweep rSweep = new RightSweep(a, c, b, row);
        for (int i=0; i<count; i++) {
            currentTime = System.currentTimeMillis();        
            vars = sweepSolve(rSweep);
            now = System.currentTimeMillis();
            fullTime[0]+=(now-currentTime);
            System.out.println("Total time Right Sweep solv: " + (now - currentTime));
        }
        verify(vars, a, c, b, row);
        
        //Левая прогонка
        Sweep lSweep = new LeftSweep(a, c, b, row);
        for (int i=0; i<count; i++) {
            currentTime = System.currentTimeMillis();
            vars = sweepSolve(lSweep);
            now = System.currentTimeMillis();
            fullTime[1]+=(now-currentTime);
            System.out.println("Total time Left Sweep solv: " + (now - currentTime));
        }
        verify(vars, a, c, b, row);
        
        //Встречная прогонка
        try (Sweep sweep = new CounterSweep(a, c, b, row)) {
            for (int i=0; i<count; i++) {
                currentTime = System.currentTimeMillis();
                vars = sweepSolve(sweep);
                now = System.currentTimeMillis();
                fullTime[2]+=(now-currentTime);
                System.out.println("Total time Counter Sweep solv: " + (now - currentTime));
            }
        }
        verify(vars, a, c, b, row);

        //Блочная прогонка
        try (Sweep sweep = new BlockSweep(a, c, b, row, 8)) {
            for (int i=0; i<count; i++) {
                currentTime = System.currentTimeMillis();
                vars = sweepSolve(sweep);
                now = System.currentTimeMillis();
                fullTime[3]+=(now-currentTime);
                System.out.println("Total time Block Sweep solv: " + (now - currentTime));
            }
        }
        verify(vars, a, c, b, row);
//        
//        System.out.println(String.format("\n\nAvarage time Right Sweep solv: %s"
//                + "\nAvarage time Left Sweep solv: %s"
//                + "\nAvarage time Counter Sweep solv: %s", fullTime[0]/count, fullTime[1]/count, fullTime[2]/count));
    }

    private static ArrayList<Double> sweepSolve(Sweep sweep) {
        sweep.solve();
        ArrayList<Double> vars = sweep.getResult();
        System.out.println("Reselt: ");
        for (Double var : vars) {
            System.out.print(var + "\t");
        }
        System.out.println();
        return vars;
    }
    
    private static void verify(ArrayList<Double> vars, ArrayList<Double> a,
            ArrayList<Double> c, ArrayList<Double> b,
            ArrayList<Double> rightPart) {
        System.out.println("Verification");
        for (int i=0; i<c.size(); i++) {
            double div = a.get(i)*((i-1>=0)?vars.get(i-1):0) + c.get(i)*vars.get(i)
                    + b.get(i)*((i+1<c.size())?vars.get(i+1):0) - rightPart.get(i);
            System.out.print(div + "\t");
        }
        System.out.println();
    }
}
