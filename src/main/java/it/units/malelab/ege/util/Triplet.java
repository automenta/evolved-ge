/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.util;

import java.util.Objects;

/**
 *
 * @author eric
 */
public class Triplet<F, S, T>  {

  public final F first;
  public final S second;
  public final T third;
  private final int hash;

  public Triplet(F first, S second, T third) {
    this.first = first;
    this.second = second;
    this.third = third;
    int hash = 5;
    hash = 83 * hash + this.first.hashCode();
    hash = 83 * hash + this.second.hashCode();
    hash = 67 * hash + this.third.hashCode();
    this.hash = hash;
  }

  @Override
  public int hashCode() {
   return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Triplet)) return false;

    final Triplet other = (Triplet) obj;
    if (hash!=other.hash) return false;

    return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second)
            && Objects.equals(this.third, other.third)
            ;
  }

  @Override
  public String toString() {
    return "<" + first + ", " + second + ", " + third + '>';
  }

}
