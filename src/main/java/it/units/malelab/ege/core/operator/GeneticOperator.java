/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.operator;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

/**
 *
 * @author eric
 */
public interface GeneticOperator<G> extends Serializable {
  
  List<G> apply(List<G> parents, Random random);
  int getParentsArity();
  int getChildrenArity();
  
}
