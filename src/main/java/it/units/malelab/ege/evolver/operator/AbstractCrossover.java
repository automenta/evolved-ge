/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.evolver.operator;

import it.units.malelab.ege.evolver.genotype.Genotype;
import java.util.Random;

/**
 *
 * @author eric
 */
public abstract class AbstractCrossover<G extends Genotype> extends AbstractOperator<G> {

  public AbstractCrossover(Random random) {
    super(random);
  }

  @Override
  public int getParentsArity() {
    return 2;
  }

  @Override
  public int getChildrenArity() {
    return 2;
  }
  
  
  
}
