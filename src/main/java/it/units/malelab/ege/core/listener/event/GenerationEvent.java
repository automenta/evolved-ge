/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.listener.event;

import it.units.malelab.ege.core.evolver.Evolver;
import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.fitness.Fitness;
import java.util.List;
import java.util.Map;

/**
 *
 * @author eric
 */
public class GenerationEvent<G, T, F extends Fitness> extends EvolutionEvent<G, T, F> {
  
  private final List<List<Individual<G, T, F>>> rankedPopulation;

  public GenerationEvent(List<List<Individual<G, T, F>>> rankedPopulation, int generation, Evolver<G, T, F> evolver, Map<String, Object> data) {
    super(generation, evolver, data);
    this.rankedPopulation = rankedPopulation;
  }

  public List<List<Individual<G, T, F>>> getRankedPopulation() {
    return rankedPopulation;
  }

}
