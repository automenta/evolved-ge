/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.evolver;

import it.units.malelab.ege.evolver.fitness.Fitness;
import it.units.malelab.ege.evolver.listener.EvolutionListener;
import it.units.malelab.ege.evolver.event.BirthEvent;
import it.units.malelab.ege.evolver.event.GenerationEvent;
import it.units.malelab.ege.evolver.event.EvolutionEndEvent;
import it.units.malelab.ege.evolver.event.MappingEvent;
import it.units.malelab.ege.evolver.event.OperatorApplicationEvent;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import it.units.malelab.ege.evolver.genotype.Genotype;
import it.units.malelab.ege.grammar.Node;
import it.units.malelab.ege.util.Utils;
import it.units.malelab.ege.evolver.operator.GeneticOperator;
import it.units.malelab.ege.mapper.MappingException;
import it.units.malelab.ege.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author eric
 */
public class StandardEvolver<G extends Genotype, T> implements Evolver<G, T> {

  protected static final int CACHE_SIZE = 10000;

  private final StandardConfiguration<G, T> configuration;
  protected final ExecutorService executor;
  protected final Random random;
  private final boolean saveAncestry;

  public StandardEvolver(int numberOfThreads, StandardConfiguration<G, T> configuration, Random random, boolean saveAncestry) {
    this.configuration = configuration;
    executor = Executors.newFixedThreadPool(numberOfThreads);
    this.random = random;
    this.saveAncestry = saveAncestry;
  }

  @Override
  public StandardConfiguration<G, T> getConfiguration() {
    return configuration;
  }

  @Override
  public void go(List<EvolutionListener<G, T>> listeners) throws InterruptedException, ExecutionException {
    LoadingCache<G, Pair<Node<T>, Map<String, Object>>> mappingCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build(getMappingCacheLoader());
    LoadingCache<Node<T>, Fitness> fitnessCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build(getFitnessCacheLoader());
    //initialize population
    int births = 0;
    List<Callable<List<Individual<G, T>>>> tasks = new ArrayList<>();
    for (G genotype : configuration.getPopulationInitializer().getGenotypes(configuration.getPopulationSize(), configuration.getInitGenotypeValidator())) {
      tasks.add(individualFromGenotypeCallable(genotype, 0, mappingCache, fitnessCache, listeners, null, null));
      births = births + 1;
    }
    List<Individual<G, T>> population = new ArrayList<>(Utils.getAll(executor.invokeAll(tasks)));
    int lastBroadcastGeneration = (int) Math.floor(births / configuration.getPopulationSize());
    Utils.broadcast(new GenerationEvent<>(population, lastBroadcastGeneration, this, null), listeners);
    //iterate
    while (Math.round(births / configuration.getPopulationSize()) < configuration.getNumberOfGenerations()) {
      int currentGeneration = (int) Math.floor(births / configuration.getPopulationSize());
      tasks.clear();
      //produce offsprings
      int i = 0;
      while (i < configuration.getOffspringSize()) {
        GeneticOperator<G> operator = Utils.selectRandom(configuration.getOperators(), random);
        List<Individual<G, T>> parents = new ArrayList<>(operator.getParentsArity());
        for (int j = 0; j < operator.getParentsArity(); j++) {
          parents.add(configuration.getParentSelector().select(population));
        }
        tasks.add(operatorApplicationCallable(operator, parents, currentGeneration, mappingCache, fitnessCache, listeners));
        i = i + operator.getChildrenArity();
      }
      List<Individual<G, T>> newPopulation = new ArrayList<>(Utils.getAll(executor.invokeAll(tasks)));
      births = births + newPopulation.size();
      //build new population
      if (configuration.isOverlapping()) {
        population.addAll(newPopulation);
      } else {
        if (newPopulation.size() >= configuration.getPopulationSize()) {
          population = newPopulation;
        } else {
          int targetSize = population.size() - newPopulation.size();
          while (population.size() > targetSize) {
            Individual<G, T> individual = configuration.getUnsurvivalSelector().select(population);
            population.remove(individual);
          }
          population.addAll(newPopulation);
        }
      }
      //select survivals
      while (population.size() > configuration.getPopulationSize()) {
        Individual<G, T> individual = configuration.getUnsurvivalSelector().select(population);
        population.remove(individual);
      }
      if ((int) Math.floor(births / configuration.getPopulationSize()) > lastBroadcastGeneration) {
        lastBroadcastGeneration = (int) Math.floor(births / configuration.getPopulationSize());
        Utils.broadcast(new GenerationEvent<>(population, lastBroadcastGeneration, this, null), listeners);
      }
    }
    //end
    Utils.broadcast(new EvolutionEndEvent<>(population, configuration.getNumberOfGenerations(), this, null), listeners);
    executor.shutdown();
  }

  protected CacheLoader<G, Pair<Node<T>, Map<String, Object>>> getMappingCacheLoader() {
    return new CacheLoader<G, Pair<Node<T>, Map<String, Object>>>() {
      @Override
      public Pair<Node<T>, Map<String, Object>> load(G genotype) throws Exception {
        Node<T> phenotype = null;
        Map<String, Object> report = new LinkedHashMap<>();
        try {
          phenotype = configuration.getMapper().map(genotype, report);
        } catch (MappingException ex) {
          phenotype = Node.EMPTY_TREE;
        }
        return new Pair<>(phenotype, report);
      }
    };
  }

  protected CacheLoader<Node<T>, Fitness> getFitnessCacheLoader() {
    return new CacheLoader<Node<T>, Fitness>() {
      @Override
      public Fitness load(Node<T> phenotype) throws Exception {
        if (Node.EMPTY_TREE.equals(phenotype)) {
          return configuration.getFitnessComputer().worstValue();
        }
        return configuration.getFitnessComputer().compute(phenotype);
      }
    };
  }

  protected Callable<List<Individual<G, T>>> individualFromGenotypeCallable(
          final G genotype,
          final int generation,
          final LoadingCache<G, Pair<Node<T>, Map<String, Object>>> mappingCache,
          final LoadingCache<Node<T>, Fitness> fitnessCache,
          final List<EvolutionListener<G, T>> listeners,
          final GeneticOperator<G> operator,
          final List<Individual<G, T>> parents) {
    final Evolver<G, T> evolver = this;
    return new Callable<List<Individual<G, T>>>() {
      @Override
      public List<Individual<G, T>> call() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Pair<Node<T>, Map<String, Object>> mappingOutcome = mappingCache.getUnchecked(genotype);
        Node<T> phenotype = mappingOutcome.getFirst();
        long elapsed = stopwatch.stop().elapsed(TimeUnit.NANOSECONDS);
        Utils.broadcast(new MappingEvent<>(genotype, phenotype, elapsed, generation, evolver, null), listeners);
        stopwatch.reset().start();
        Fitness fitness = fitnessCache.getUnchecked(phenotype);
        elapsed = stopwatch.stop().elapsed(TimeUnit.NANOSECONDS);
        Individual<G, T> individual = new Individual<>(genotype, phenotype, fitness, generation, operator, saveAncestry ? parents : null, mappingOutcome.getSecond());
        Utils.broadcast(new BirthEvent<>(individual, elapsed, generation, evolver, null), listeners);
        return Collections.singletonList(individual);
      }
    };
  }

  protected Callable<List<Individual<G, T>>> operatorApplicationCallable(
          final GeneticOperator<G> operator,
          final List<Individual<G, T>> parents,
          final int generation,
          final LoadingCache<G, Pair<Node<T>, Map<String, Object>>> mappingCache,
          final LoadingCache<Node<T>, Fitness> fitnessCache,
          final List<EvolutionListener<G, T>> listeners
  ) {
    final Evolver<G, T> evolver = this;
    return new Callable<List<Individual<G, T>>>() {
      @Override
      public List<Individual<G, T>> call() throws Exception {
        List<Individual<G, T>> children = new ArrayList<>(operator.getChildrenArity());
        List<G> parentGenotypes = new ArrayList<>(operator.getParentsArity());
        for (Individual<G, T> parent : parents) {
          parentGenotypes.add(parent.getGenotype());
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<G> childGenotypes = operator.apply(parentGenotypes).subList(0, operator.getChildrenArity());
        long elapsed = stopwatch.elapsed(TimeUnit.NANOSECONDS);
        for (G childGenotype : childGenotypes) {
          children.addAll(individualFromGenotypeCallable(childGenotype, generation, mappingCache, fitnessCache, listeners, operator, parents).call());
        }
        Utils.broadcast(new OperatorApplicationEvent<>(parents, children, operator, elapsed, generation, evolver, null), listeners);
        return children;
      }
    };
  }

}
