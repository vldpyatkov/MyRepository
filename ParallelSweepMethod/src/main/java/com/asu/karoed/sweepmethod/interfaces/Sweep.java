/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.asu.karoed.sweepmethod.interfaces;

import java.io.Closeable;
import java.util.ArrayList;

/**
 *
 * @author makros
 */
public interface Sweep extends Closeable {

    ArrayList<Double> getResult();

    void solve();
    
}
