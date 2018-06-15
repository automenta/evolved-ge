/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.evolver.sepandconq;

import it.units.malelab.ege.core.Node;

/**
 *
 * @author eric
 */
public interface Joiner<T> {
  
  Node<T> join(Node<T>... pieces);
  
}
