/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.benchmark.symbolicregression.element;

/**
 *
 * @author eric
 */
public class Constant implements Element {
  
  private final double value;

  public Constant(double value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return Double.toString(value);
  }

  public double getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 73 * hash + (int) (Double.doubleToLongBits(this.value) ^ (Double.doubleToLongBits(this.value) >>> 32));
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Constant other = (Constant) obj;
      return Double.doubleToLongBits(this.value) == Double.doubleToLongBits(other.value);
  }
  
}
