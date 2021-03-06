/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.listener.collector;

import it.units.malelab.ege.core.Sequence;
import it.units.malelab.ege.core.fitness.FitnessComputer;
import it.units.malelab.ege.core.fitness.NumericFitness;
import java.util.Collections;
import java.util.Map;

/**
 *
 * @author eric
 */
public class NumericFirstBest<G extends Sequence, T> extends Best<G, T, NumericFitness> {

  private final String fitnessFormat;

  public NumericFirstBest(boolean ancestry, FitnessComputer<T, NumericFitness> validationFitnessComputer, String fitnessFormat) {
    super(ancestry, validationFitnessComputer);
    this.fitnessFormat = fitnessFormat;
  }

  @Override
  protected Map<String, String> getFitnessFormattedNames() {
    return Collections.singletonMap("", fitnessFormat);
  }

  @Override
  protected Map<String, Object> getFitnessIndexes(NumericFitness fitness) {
    return (Map)Collections.singletonMap("", fitness.getValue());
  }

}
