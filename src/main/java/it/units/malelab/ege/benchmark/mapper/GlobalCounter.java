/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.benchmark.mapper;

/**
 *
 * @author eric
 */
class GlobalCounter {
  
  private int c = 0;
  
  public int r() {
    return c;
  }
  
  public int rw() {
    c = c+1;
    return c-1;
  }

  @Override
  public String toString() {
    return Integer.toString(c);
  }
  
}
