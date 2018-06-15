/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.listener.collector;

import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.fitness.Fitness;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.listener.event.GenerationEvent;

import java.util.*;

/**
 *
 * @author eric
 */
public class Diversity<G, T, F extends Fitness> implements Collector<G, T, F> {

  @Override
  public Map<String, Object> collect(GenerationEvent<G, T, F> event) {
    List<List<Individual<G, T, F>>> rankedPopulation = new ArrayList<>(event.getRankedPopulation());
    int n = rankedPopulation.size();
    Collection<G> genotypes = new HashSet<>(n);
    Collection<Node<T>> phenotypes = new HashSet<>(n);
    Collection<F> fitnesses = new HashSet<>(n);
    double count = 0;
    for (List<Individual<G, T, F>> rank : rankedPopulation) {
      for (Individual<G, T, F> individual : rank) {
        genotypes.add(individual.genotype);
        phenotypes.add(individual.phenotype);
          fitnesses.add(individual.fitness);
        count = count+1;
      }
    }
    Map<String, Object> indexes = new LinkedHashMap<>(n*3);
    indexes.put("diversity.genotype", genotypes.size() / count);
    indexes.put("diversity.phenotype", phenotypes.size() / count);
    indexes.put("diversity.fitness", fitnesses.size() / count);
    return indexes;
  }

  @Override
  public Map<String, String> getFormattedNames() {
    Map<String, String> formattedNames = new LinkedHashMap<>();
    formattedNames.put("diversity.genotype", "%4.2f");
    formattedNames.put("diversity.phenotype", "%4.2f");
    formattedNames.put("diversity.fitness", "%4.2f");
    return formattedNames;
  }

}
