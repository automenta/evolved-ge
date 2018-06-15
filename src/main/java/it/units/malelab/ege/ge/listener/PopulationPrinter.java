/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.ge.listener;

import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.fitness.Fitness;
import it.units.malelab.ege.core.listener.AbstractListener;
import it.units.malelab.ege.core.listener.event.EvolutionEvent;
import it.units.malelab.ege.core.listener.event.GenerationEvent;
import it.units.malelab.ege.ge.genotype.BitsGenotype;
import it.units.malelab.ege.ge.mapper.StandardGEMapper;
import it.units.malelab.ege.util.Utils;
import java.io.PrintStream;
import java.util.List;

/**
 *
 * @author eric
 */
public class PopulationPrinter<G extends BitsGenotype, T, F extends Fitness> extends AbstractListener<G, T, F> {

  private final PrintStream ps;

  private static final char[][] CHARS = {{'O', 'I'}, {'.', ','}};

  public PopulationPrinter(PrintStream ps) {
    super(GenerationEvent.class);
    this.ps = ps;
  }

  @Override
  public void listen(EvolutionEvent<G, T, F> event) {
    int generation = event.getGeneration();
    ps.printf("Population at generation %d%n", generation);
    List<List<Individual<G, T, F>>> rankedPopulation = ((GenerationEvent) event).getRankedPopulation();
    for (List<Individual<G, T, F>> rank : rankedPopulation) {
      for (Individual<G, T, F> individual : rank) {
        G genotype = individual.genotype;
        //genotype
        int[] bitUsages = (int[]) individual.otherInfo.get(StandardGEMapper.BIT_USAGES_INDEX_NAME);
        if (bitUsages == null) {
          bitUsages = new int[genotype.leaves()];
        }
        for (int i = 0; i < genotype.leaves(); i++) {
          ps.print(CHARS[bitUsages[i] > 0 ? 0 : 1][genotype.content(i) ? 0 : 1]);
        }
          ps.printf(" -> %s%n", Utils.contents(individual.phenotype.leafNodes()));
      }
    }
  }

}
