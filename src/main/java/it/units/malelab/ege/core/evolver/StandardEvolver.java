/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.evolver;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.fitness.Fitness;
import it.units.malelab.ege.core.listener.EvolverListener;
import it.units.malelab.ege.core.listener.event.*;
import it.units.malelab.ege.core.mapper.MappingException;
import it.units.malelab.ege.core.operator.GeneticOperator;
import it.units.malelab.ege.util.Pair;
import it.units.malelab.ege.util.Utils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author eric
 */
public class StandardEvolver<G, T, F extends Fitness> implements Evolver<G, T, F> {

    public final static String MAPPING_CACHE_NAME = "mapping";
    public final static String FITNESS_CACHE_NAME = "fitness";
    protected static final int CACHE_SIZE = 200000;
    protected final boolean saveAncestry;
    private final StandardConfiguration<G, T, F> configuration;

    public StandardEvolver(StandardConfiguration<G, T, F> configuration, boolean saveAncestry) {
        this.configuration = configuration;
        this.saveAncestry = saveAncestry;
    }

    protected static Map<String, Object> cacheStats(Cache mappingCache, Cache fitnessCache) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MAPPING_CACHE_NAME, mappingCache.stats());
        map.put(FITNESS_CACHE_NAME, fitnessCache.stats());
        return map;
    }

    @Override
    public Configuration<G, T, F> getConfiguration() {
        return configuration;
    }

    @Override
    public List<Node<T>> solve(ExecutorService executor, Random random, List<EvolverListener<G, T, F>> listeners) throws InterruptedException, ExecutionException {
        LoadingCache<G, Pair<Node<T>, Map<String, Object>>> mappingCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).recordStats().build(getMappingCacheLoader());
        LoadingCache<Node<T>, F> fitnessCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).recordStats().build(getFitnessCacheLoader());
        Stopwatch stopwatch = Stopwatch.createStarted();
        //initialize population
        float births = 0;
        Collection<Callable<List<Individual<G, T, F>>>> tasks = new ArrayList<>();
        for (G genotype: configuration.getPopulationInitializer().build(configuration.getPopulationSize(), configuration.getInitGenotypeValidator(), random)) {
            tasks.add(individualFromGenotypeCallable(genotype, 0, mappingCache, fitnessCache, listeners, null, null, executor));
            births = births + 1;
        }
        List<Individual<G, T, F>> population = new ArrayList<>(Utils.getAll(executor.invokeAll(tasks)));
        int lastBroadcastGeneration = (int) Math.floor(actualBirths(births, fitnessCache) / configuration.getPopulationSize());
        Utils.broadcast(new EvolutionStartEvent<>(this, cacheStats(mappingCache, fitnessCache)), listeners, executor);
        Utils.broadcast(new GenerationEvent<>(configuration.getRanker().rank(population, random), lastBroadcastGeneration, this, cacheStats(mappingCache, fitnessCache)), listeners, executor);
        //iterate
        while (Math.round(actualBirths(births, fitnessCache) / configuration.getPopulationSize()) < configuration.getNumberOfGenerations()) {
            int currentGeneration = (int) Math.floor(births / configuration.getPopulationSize());
            tasks.clear();
            //re-rank
            List<List<Individual<G, T, F>>> rankedPopulation = configuration.getRanker().rank(population, random);
            //produce offsprings
            int i = 0;
            while (i < configuration.getOffspringSize()) {
                GeneticOperator<G> operator = Utils.selectRandom(configuration.getOperators(), random);
                List<Individual<G, T, F>> parents = new ArrayList<>(operator.getParentsArity());
                for (int j = 0; j < operator.getParentsArity(); j++) {
                    parents.add(configuration.getParentSelector().select(rankedPopulation, random));
                }
                tasks.add(operatorApplicationCallable(operator, parents, random, currentGeneration, mappingCache, fitnessCache, listeners, executor));
                i = i + operator.getChildrenArity();
            }
            List<Individual<G, T, F>> newPopulation = new ArrayList<>(Utils.getAll(executor.invokeAll(tasks)));
            births = births + newPopulation.size();
            //build new population
            if (configuration.isOverlapping()) {
                population.addAll(newPopulation);
            } else {
                if (newPopulation.size() >= configuration.getPopulationSize()) {
                    population = newPopulation;
                } else {
                    //keep missing individuals from old population
                    int targetSize = population.size() - newPopulation.size();
                    while (population.size() > targetSize) {
                        Individual<G, T, F> individual = configuration.getUnsurvivalSelector().select(rankedPopulation, random);
                        population.remove(individual);
                    }
                    population.addAll(newPopulation);
                }
            }
            //select survivals
            while (population.size() > configuration.getPopulationSize()) {
                //re-rank
                rankedPopulation = configuration.getRanker().rank(population, random);
                Individual<G, T, F> individual = configuration.getUnsurvivalSelector().select(rankedPopulation, random);
                population.remove(individual);
            }
            if ((int) Math.floor(actualBirths(births, fitnessCache) / configuration.getPopulationSize()) > lastBroadcastGeneration) {
                lastBroadcastGeneration = (int) Math.floor(actualBirths(births, fitnessCache) / configuration.getPopulationSize());
                Utils.broadcast(new GenerationEvent<>((List) rankedPopulation, lastBroadcastGeneration, this, cacheStats(mappingCache, fitnessCache)), listeners, executor);
            }
            if (configuration.getMaxRelativeElapsed() > 0) {
                //check if relative elapsed time exceeded
                double avgFitnessComputationNanos = fitnessCache.stats().averageLoadPenalty();
                double elapsedNanos = stopwatch.elapsed(TimeUnit.NANOSECONDS);
                if (elapsedNanos / avgFitnessComputationNanos > configuration.getMaxRelativeElapsed()) {
                    break;
                }
            }
            if (configuration.getMaxElapsed() > 0) {
                //check if elapsed time exceeded
                if (stopwatch.elapsed(TimeUnit.SECONDS) > configuration.getMaxElapsed()) {
                    break;
                }
            }
            if (configuration.getProblem().getLearningFitnessComputer().bestValue() != null) {
                //check if optimal solution found
                if (rankedPopulation.get(0).get(0).fitness.equals(configuration.getProblem().getLearningFitnessComputer().bestValue())) {
                    break;
                }
            }
        }
        //end
        List<Node<T>> bestPhenotypes = new ArrayList<>();
        List<List<Individual<G, T, F>>> rankedPopulation = configuration.getRanker().rank(population, random);
        Utils.broadcast(new EvolutionEndEvent<>((List) rankedPopulation, (int) Math.floor(actualBirths(births, fitnessCache) / configuration.getPopulationSize()), this, cacheStats(mappingCache, fitnessCache)), listeners, executor);
        for (Individual<G, T, F> individual: rankedPopulation.get(0)) {
            bestPhenotypes.add(individual.phenotype);
        }
        return bestPhenotypes;
    }

    protected float actualBirths(float births, Cache<Node<T>, F> fitnessCache) {
        return configuration.isActualEvaluations() ? (int) fitnessCache.stats().missCount() : births;
    }

    protected CacheLoader<G, Pair<Node<T>, Map<String, Object>>> getMappingCacheLoader() {
        return new CacheLoader<>() {
            @Override
            public Pair<Node<T>, Map<String, Object>> load(G genotype) {
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

    protected CacheLoader<Node<T>, F> getFitnessCacheLoader() {
        return new CacheLoader<>() {
            @Override
            public F load(Node<T> phenotype) {
                if (Node.EMPTY_TREE.equals(phenotype)) {
                    return configuration.getProblem().getLearningFitnessComputer().worstValue();
                }
                return configuration.getProblem().getLearningFitnessComputer().compute(phenotype);
            }
        };
    }

    protected Callable<List<Individual<G, T, F>>> individualFromGenotypeCallable(
            final G genotype,
            final int generation,
            final LoadingCache<G, Pair<Node<T>, Map<String, Object>>> mappingCache,
            final LoadingCache<Node<T>, F> fitnessCache,
            final Iterable<EvolverListener<G, T, F>> listeners,
            final GeneticOperator<G> operator,
            final Collection<Individual<G, T, F>> parents,
            final ExecutorService executor) {

        if (genotype == null)
            return null;

        final Evolver<G, T, F> evolver = this;
        return () -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            Pair<Node<T>, Map<String, Object>> mappingOutcome = mappingCache.getUnchecked(genotype);
            Node<T> phenotype = mappingOutcome.first;
            long elapsed = stopwatch.stop().elapsed(TimeUnit.NANOSECONDS);
            Utils.broadcast(new MappingEvent<>(genotype, phenotype, elapsed, generation, evolver, null), listeners, executor);
            stopwatch.reset().start();
            F fitness = fitnessCache.getUnchecked(phenotype);
            elapsed = stopwatch.stop().elapsed(TimeUnit.NANOSECONDS);
            Individual<G, T, F> individual = new Individual<>(genotype, phenotype, fitness, generation, saveAncestry ? parents : null, mappingOutcome.second);
            Utils.broadcast(new BirthEvent<>(individual, elapsed, generation, evolver, null), listeners, executor);
            return List.of(individual);
        };
    }

    Callable<List<Individual<G, T, F>>> operatorApplicationCallable(
            final GeneticOperator<G> operator,
            final List<Individual<G, T, F>> parents,
            final Random random,
            final int generation,
            final LoadingCache<G, Pair<Node<T>, Map<String, Object>>> mappingCache,
            final LoadingCache<Node<T>, F> fitnessCache,
            final Iterable<EvolverListener<G, T, F>> listeners,
            final ExecutorService executor
    ) {
        final Evolver<G, T, F> evolver = this;
        return () -> {
            List<Individual<G, T, F>> children = new ArrayList<>(operator.getChildrenArity());
            List<G> parentGenotypes = new ArrayList<>(operator.getParentsArity());
            for (Individual<G, T, F> parent: parents) {
                parentGenotypes.add(parent.genotype);
            }
            Stopwatch stopwatch = Stopwatch.createStarted();
            List<G> result = operator.apply(parentGenotypes, random);
            if (result != null) {
                List<G> childGenotypes = result.subList(0, operator.getChildrenArity());
                long elapsed = stopwatch.elapsed(TimeUnit.NANOSECONDS);
                if (childGenotypes != null) {
                    for (G childGenotype: childGenotypes) {
                        children.addAll(individualFromGenotypeCallable(childGenotype, generation, mappingCache, fitnessCache, listeners, operator, parents, executor).call());
                    }
                }
                Utils.broadcast(new OperatorApplicationEvent<>(parents, children, operator, elapsed, generation, evolver, null), listeners, executor);
            }
            return children;
        };
    }
}
