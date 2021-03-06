/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.evolver;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.util.Utils;
import it.units.malelab.ege.core.fitness.Fitness;
import it.units.malelab.ege.core.listener.EvolverListener;
import it.units.malelab.ege.core.listener.event.EvolutionEndEvent;
import it.units.malelab.ege.core.listener.event.EvolutionStartEvent;
import it.units.malelab.ege.core.listener.event.GenerationEvent;
import it.units.malelab.ege.core.ranker.Ranker;
import it.units.malelab.ege.core.selector.Selector;
import it.units.malelab.ege.core.operator.GeneticOperator;
import it.units.malelab.ege.util.Pair;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author eric
 */
public class PartitionEvolver<G, T, F extends Fitness> extends StandardEvolver<G, T, F> {

  private final PartitionConfiguration<G, T, F> configuration;

  public PartitionEvolver(PartitionConfiguration<G, T, F> configuration, boolean saveAncestry) {
    super(configuration, saveAncestry);
    this.configuration = configuration;
  }

  @Override
  public List<Node<T>> solve(ExecutorService executor, Random random, List<EvolverListener<G, T, F>> listeners) throws InterruptedException, ExecutionException {
    LoadingCache<G, Pair<Node<T>, Map<String, Object>>> mappingCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build(getMappingCacheLoader());
    LoadingCache<Node<T>, F> fitnessCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build(getFitnessCacheLoader());
    //initialize population
    int births = 0;
    Collection<Callable<List<Individual<G, T, F>>>> tasks = new ArrayList<>();
    for (G genotype : configuration.getPopulationInitializer().build(configuration.getPopulationSize(), configuration.getInitGenotypeValidator(), random)) {
      tasks.add(individualFromGenotypeCallable(genotype, 0, mappingCache, fitnessCache, listeners, null, null, executor));
      births = births + 1;
    }
    List<List<Individual<G, T, F>>> partitionedPopulation = new ArrayList<>();
    for (Individual<G, T, F> individual : Utils.getAll(executor.invokeAll(tasks))) {
      addToPartition(partitionedPopulation, individual);
    }
    //trim partitions
    trimPartitions(partitionedPopulation, random);
    int lastBroadcastGeneration = (int) Math.floor(actualBirths(births, fitnessCache) / configuration.getPopulationSize());
    Utils.broadcast(new EvolutionStartEvent<>(this, cacheStats(mappingCache, fitnessCache)), listeners, executor);
    Utils.broadcast(new GenerationEvent<>(configuration.getRanker().rank(all(partitionedPopulation), random), lastBroadcastGeneration, this, cacheStats(mappingCache, fitnessCache)), listeners, executor);
    //iterate
    while (Math.round(actualBirths(births, fitnessCache) / configuration.getPopulationSize()) < configuration.getNumberOfGenerations()) {
      int currentGeneration = (int) Math.floor(actualBirths(births, fitnessCache) / configuration.getPopulationSize());
      tasks.clear();
      //re-rank
      Map<Individual<G, T, F>, List<Individual<G, T, F>>> parentRepresentedPartitions = representedPartitions(
              configuration.getParentInPartitionRanker(),
              configuration.getParentInPartitionSelector(),
              partitionedPopulation,
              random);
      List<List<Individual<G, T, F>>> parentRankedRepresenters = rankRepresenters(
              (Ranker) configuration.getRanker(),
              parentRepresentedPartitions,
              random);
      //produce offsprings
      int i = 0;
      while (i < configuration.getOffspringSize()) {
        GeneticOperator<G> operator = Utils.selectRandom(configuration.getOperators(), random);
        List<Individual<G, T, F>> parents = new ArrayList<>(operator.getParentsArity());
        for (int j = 0; j < operator.getParentsArity(); j++) {
          parents.add(configuration.getParentSelector().select(parentRankedRepresenters, random));
        }
        tasks.add(operatorApplicationCallable(operator, parents, random, currentGeneration, mappingCache, fitnessCache, listeners, executor));
        i = i + operator.getChildrenArity();
      }
      Collection<Individual<G, T, F>> newPopulation = new ArrayList<>(Utils.getAll(executor.invokeAll(tasks)));
      births = births + newPopulation.size();
      //build new population
      if (configuration.isOverlapping()) {
        for (Individual<G, T, F> individual : newPopulation) {
          addToPartition(partitionedPopulation, individual);
        }
      } else {
        List<List<Individual<G, T, F>>> newPartitionedPopulation = new ArrayList<>();
        for (Individual<G, T, F> individual : newPopulation) {
          addToPartition(newPartitionedPopulation, individual);
        }
        //keep missing individuals from old population
        for (List<Individual<G, T, F>> oldRank : parentRankedRepresenters) {
          if (newPartitionedPopulation.size() >= configuration.getPopulationSize()) {
            break;
          }
          for (Individual<G, T, F> oldRepresenter : oldRank) {
            if (newPartitionedPopulation.size() >= configuration.getPopulationSize()) {
              break;
            }
            for (Individual<G, T, F> oldIndividual : parentRepresentedPartitions.get(oldRepresenter)) {
              if (newPartitionedPopulation.size() >= configuration.getPopulationSize()) {
                break;
              }
              addToPartition(newPartitionedPopulation, oldIndividual);
            }
          }
        }
        partitionedPopulation = newPartitionedPopulation;
      }
      //select survivals
      while (partitionedPopulation.size() > configuration.getPopulationSize()) {
      //re-rank
      Map<Individual<G, T, F>, List<Individual<G, T, F>>> unsurvivalRepresentedPartitions = representedPartitions(
              configuration.getUnsurvivalInPartitionRanker(),
              configuration.getUnsurvivalInPartitionSelector(),
              partitionedPopulation,
              random);
      List<List<Individual<G, T, F>>> unsurvivalRankedRepresenters = rankRepresenters(
              (Ranker) configuration.getRanker(),
              unsurvivalRepresentedPartitions,
              random);
        partitionedPopulation.remove(unsurvivalRepresentedPartitions.get(configuration.getUnsurvivalSelector().select(unsurvivalRankedRepresenters, random)));
      }
      //trim partitions
      trimPartitions(partitionedPopulation, random);
      if ((int) Math.floor(actualBirths(births, fitnessCache) / configuration.getPopulationSize()) > lastBroadcastGeneration) {
        lastBroadcastGeneration = (int) Math.floor(actualBirths(births, fitnessCache) / configuration.getPopulationSize());
        Utils.broadcast(new GenerationEvent<>(configuration.getRanker().rank(all(partitionedPopulation), random), lastBroadcastGeneration, this, cacheStats(mappingCache, fitnessCache)), listeners, executor);
      }
    }
    //end
    Utils.broadcast(new EvolutionEndEvent<>(configuration.getRanker().rank(all(partitionedPopulation), random), configuration.getNumberOfGenerations(), this, cacheStats(mappingCache, fitnessCache)), listeners, executor);
    List<Node<T>> bestPhenotypes = new ArrayList<>();
    List<List<Individual<G, T, F>>> rankedPopulation = configuration.getRanker().rank(all(partitionedPopulation), random);
    for (Individual<G, T, F> individual : rankedPopulation.get(0)) {
        bestPhenotypes.add(individual.phenotype);
    }
    return bestPhenotypes;
  }

  private void trimPartitions(Iterable<List<Individual<G, T, F>>> partitionedPopulation, Random random) {
    for (List<Individual<G, T, F>> partition : partitionedPopulation) {
      while (partition.size()>configuration.getPartitionSize()) {
        List<List<Individual<G, T, F>>> rankedPartition = configuration.getUnsurvivalInPartitionRanker().rank(partition, random);
        Individual<G, T, F> unsurvival = configuration.getUnsurvivalInPartitionSelector().select(rankedPartition, random);
        partition.remove(unsurvival);
      }
    }
  }

  private void addToPartition(Collection<List<Individual<G, T, F>>> partitionedPopulation, Individual<G, T, F> individual) {
    boolean found = false;
    for (List<Individual<G, T, F>> partition : partitionedPopulation) {
      if (configuration.getPartitionerComparator().compare(individual, partition.get(0)) == 0) {
        found = true;
        partition.add(individual);
        break;
      }
    }
    if (!found) {
      List<Individual<G, T, F>> newPartition = new ArrayList<>();
      newPartition.add(individual);
      partitionedPopulation.add(newPartition);
    }
  }

  private static <K> List<K> all(Collection<List<K>> partitions) {
    List<K> all = new ArrayList<>(partitions.size());
    for (List<K> partition : partitions) {
      all.addAll(partition);
    }
    return all;
  }

  private static <K> List<List<K>> rankRepresenters(Ranker<K> ranker, Map<K, List<K>> representedPartitions, Random random) {
    return ranker.rank(new ArrayList<>(representedPartitions.keySet()), random);
  }

  private static <K> Map<K, List<K>> representedPartitions(Ranker<K> ranker, Selector<K> selector, Iterable<List<K>> partitions, Random random) {
    Map<K, List<K>> representedPartitions = new LinkedHashMap<>();
    for (List<K> partition : partitions) {      
      representedPartitions.put(selector.select(ranker.rank(partition, random), random), partition);
    }
    return representedPartitions;
  }

}
