package com.asu.dmsaas.tasktemplates.embedded.io.csv.parallelsweepmethod;

import com.asu.karoed.sweepmethod.sequential.RightSweep;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
        rightSweepTest();
    }

    private ArrayList<Double> toList(double[] arr) {
        ArrayList<Double> list = new ArrayList<Double>();
        for (double num : arr) {
            list.add(new Double(num));
        }
        return list;
    }

    public void rightSweepTest() {
        double[] upDiagonal = {3, 1, -2, 0};
        double[] diagonal = {5, 6, 4, -3};
        double[] downDiagonal = {0, 3, 1, 1};
        double[] rightPart = {8, 10, 3, -2};
        ArrayList<Double> a = toList(upDiagonal);
        ArrayList<Double> b = toList(diagonal);
        ArrayList<Double> c = toList(downDiagonal);
        ArrayList<Double> d = toList(rightPart);

        RightSweep rightSweep = new RightSweep(c, b, a, d);
        rightSweep.solve();
        ArrayList<Double> result = rightSweep.getResult();

        System.out.print("\n[ ");
        for (Double r : result) {
            System.out.print(r + " ");
        }
        System.out.println(']');

    }
}
