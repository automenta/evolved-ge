/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.benchmark.booleanfunction.element;

import java.util.Objects;

/**
 *
 * @author eric
 */
public class Variable implements Element {
  
  private final String string;

  public Variable(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 37 * hash + Objects.hashCode(this.string);
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
    final Variable other = (Variable) obj;
    if (!Objects.equals(this.string, other.string)) {
      return false;
    }
    return true;
  }
  
}
