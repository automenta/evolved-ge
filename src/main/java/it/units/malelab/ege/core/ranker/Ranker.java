/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.ranker;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author eric
 */
public interface Ranker<T> extends Serializable {
  
  List<List<T>> rank(List<T> ts, Random random);
  
}
