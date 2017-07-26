/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.util.distance;

import it.units.malelab.ege.core.Sequence;

/**
 *
 * @author eric
 */
public class Hamming<T> implements Distance<Sequence<T>>{

  @Override
  public double d(Sequence<T> t1, Sequence<T> t2) {
    if (t1.size()!=t2.size()) {
      throw new IllegalArgumentException(String.format("Sequences size should be the same (%d vs. %d)", t1.size(), t2.size()));
    }
    int count = 0;
    for (int i = 0; i<t1.size(); i++) {
      if (!t1.get(i).equals(t2.get(i))) {
        count = count+1;
      }
    }
    return count;
  }
  
  
}
