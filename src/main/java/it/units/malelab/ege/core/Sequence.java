/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core;

import java.io.Serializable;

/**
 *
 * @author eric
 */
public interface Sequence<T> extends Serializable {
  
  T content(int index);
  void replace(int index, T t);
  int leaves();
  Sequence<T> clone();
  
}
