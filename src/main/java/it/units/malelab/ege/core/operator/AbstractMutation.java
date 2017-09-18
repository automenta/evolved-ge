/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.operator;

import java.util.Random;

/**
 *
 * @author eric
 */
public abstract class AbstractMutation<G> implements GeneticOperator<G> {

  @Override
  public int getParentsArity() {
    return 1;
  }
  
  @Override
  public int getChildrenArity() {
    return 1;
  }
  
}
