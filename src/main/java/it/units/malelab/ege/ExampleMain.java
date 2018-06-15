/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege;

import it.units.malelab.ege.benchmark.KLandscapes;
import it.units.malelab.ege.cfggp.initializer.FullTreeFactory;
import it.units.malelab.ege.cfggp.initializer.GrowTreeFactory;
import it.units.malelab.ege.cfggp.mapper.CfgGpMapper;
import it.units.malelab.ege.cfggp.operator.StandardTreeCrossover;
import it.units.malelab.ege.cfggp.operator.StandardTreeMutation;
import it.units.malelab.ege.core.evolver.Evolver;
import it.units.malelab.ege.core.Problem;
import it.units.malelab.ege.core.fitness.NumericFitness;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.listener.CollectorGenerationLogger;
import it.units.malelab.ege.core.listener.EvolverListener;
import it.units.malelab.ege.core.listener.collector.NumericFirstBest;
import it.units.malelab.ege.core.listener.collector.Population;
import it.units.malelab.ege.core.listener.collector.Diversity;
import it.units.malelab.ege.core.selector.LastWorst;
import it.units.malelab.ege.core.selector.Tournament;
import it.units.malelab.ege.core.selector.IndividualComparator;
import it.units.malelab.ege.core.evolver.StandardConfiguration;
import it.units.malelab.ege.core.evolver.StandardEvolver;
import it.units.malelab.ege.core.initializer.MultiInitializer;
import it.units.malelab.ege.core.initializer.PopulationInitializer;
import it.units.malelab.ege.core.initializer.RandomInitializer;
import it.units.malelab.ege.core.listener.collector.BestPrinter;
import it.units.malelab.ege.core.validator.Any;
import it.units.malelab.ege.core.operator.GeneticOperator;
import it.units.malelab.ege.core.ranker.ComparableRanker;
import it.units.malelab.ege.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author eric
 */
public class ExampleMain {

  public static void main(String[] args) throws InterruptedException, ExecutionException {
    solveKLandscapesCfgGp();
  }

  private static void solveKLandscapesCfgGp() throws InterruptedException, ExecutionException {
    Random random = new Random(1L);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
    Problem<String, NumericFitness> problem = new KLandscapes(8);
    int maxDepth = 16;
    StandardConfiguration<Node<String>, String, NumericFitness> configuration = new StandardConfiguration<>(
            500,
            50,
            new MultiInitializer<>(new Utils.MapBuilder<PopulationInitializer<Node<String>>, Double>()
                    .put(new RandomInitializer<>(new GrowTreeFactory<>(maxDepth, problem.getGrammar())), 0.5)
                    .put(new RandomInitializer<>(new FullTreeFactory<>(maxDepth, problem.getGrammar())), 0.5)
                    .build()
            ),
            new Any<>(),
            new CfgGpMapper<>(),
            new Utils.MapBuilder<GeneticOperator<Node<String>>, Double>()
            .put(new StandardTreeCrossover<>(maxDepth), 0.8d)
            .put(new StandardTreeMutation<>(maxDepth, problem.getGrammar()), 0.2d)
            .build(),
            new ComparableRanker<>(new IndividualComparator<>(IndividualComparator.Attribute.FITNESS)),
            new Tournament<>(3),
            new LastWorst<>(),
            500,
            true,
            problem,
            false,
            -1, -1);
    List<EvolverListener<Node<String>, String, NumericFitness>> listeners = new ArrayList<>();
    listeners.add(new CollectorGenerationLogger<>(
            Collections.emptyMap(), System.out, true, 10, " ", " | ",
            new Population<>(),
            new NumericFirstBest<>(false, problem.getTestingFitnessComputer(), "%6.2f"),
            new Diversity<>(),
            new BestPrinter<>(problem.getPhenotypePrinter(), "%30.30s")
    ));
    Evolver<Node<String>, String, NumericFitness> evolver = new StandardEvolver<>(configuration, false);
    List<Node<String>> bests = evolver.solve(executor, random, listeners);
    System.out.printf("Found %d solutions.%n", bests.size());
  }

}
