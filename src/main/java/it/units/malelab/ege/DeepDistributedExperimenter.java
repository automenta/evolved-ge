/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege;

import com.google.common.collect.Lists;
import it.units.malelab.ege.benchmark.KLandscapes;
import it.units.malelab.ege.benchmark.Text;
import it.units.malelab.ege.benchmark.booleanfunction.MultipleOutputParallelMultiplier;
import it.units.malelab.ege.benchmark.booleanfunction.Parity;
import it.units.malelab.ege.benchmark.symbolicregression.HarmonicCurve;
import it.units.malelab.ege.benchmark.symbolicregression.Nguyen7;
import it.units.malelab.ege.benchmark.symbolicregression.Pagie1;
import it.units.malelab.ege.cfggp.initializer.FullTreeFactory;
import it.units.malelab.ege.cfggp.initializer.GrowTreeFactory;
import it.units.malelab.ege.cfggp.mapper.CfgGpMapper;
import it.units.malelab.ege.cfggp.operator.StandardTreeCrossover;
import it.units.malelab.ege.cfggp.operator.StandardTreeMutation;
import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.Problem;
import it.units.malelab.ege.core.Sequence;
import it.units.malelab.ege.core.evolver.DeterministicCrowdingConfiguration;
import it.units.malelab.ege.core.evolver.PartitionConfiguration;
import it.units.malelab.ege.core.evolver.StandardConfiguration;
import it.units.malelab.ege.core.fitness.NumericFitness;
import it.units.malelab.ege.core.initializer.MultiInitializer;
import it.units.malelab.ege.core.initializer.PopulationInitializer;
import it.units.malelab.ege.core.initializer.RandomInitializer;
import it.units.malelab.ege.core.listener.collector.BestPrinter;
import it.units.malelab.ege.core.listener.collector.Diversity;
import it.units.malelab.ege.core.listener.collector.NumericFirstBest;
import it.units.malelab.ege.core.listener.collector.Population;
import it.units.malelab.ege.core.mapper.Mapper;
import it.units.malelab.ege.core.ranker.ComparableRanker;
import it.units.malelab.ege.core.ranker.RandomizerRanker;
import it.units.malelab.ege.core.ranker.Ranker;
import it.units.malelab.ege.core.selector.FirstBest;
import it.units.malelab.ege.core.selector.IndividualComparator;
import it.units.malelab.ege.core.selector.LastWorst;
import it.units.malelab.ege.core.selector.Tournament;
import it.units.malelab.ege.ge.genotype.BitsGenotypeFactory;
import it.units.malelab.ege.ge.genotype.SGEGenotypeFactory;
import it.units.malelab.ege.core.validator.Any;
import it.units.malelab.ege.distributed.Job;
import it.units.malelab.ege.distributed.master.Master;
import it.units.malelab.ege.ge.mapper.BitsSGEMapper;
import it.units.malelab.ege.ge.mapper.HierarchicalMapper;
import it.units.malelab.ege.ge.mapper.PiGEMapper;
import it.units.malelab.ege.ge.mapper.SGEMapper;
import it.units.malelab.ege.ge.mapper.StandardGEMapper;
import it.units.malelab.ege.ge.mapper.WeightedHierarchicalMapper;
import it.units.malelab.ege.ge.operator.LengthPreservingTwoPointsCrossover;
import it.units.malelab.ege.ge.operator.ProbabilisticMutation;
import it.units.malelab.ege.ge.operator.SGECrossover;
import it.units.malelab.ege.ge.operator.SGEMutation;
import it.units.malelab.ege.util.Utils;
import it.units.malelab.ege.util.distance.CachedDistance;
import it.units.malelab.ege.util.distance.Distance;
import it.units.malelab.ege.util.distance.Hamming;
import it.units.malelab.ege.util.distance.LeavesEdit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 *
 * @author eric
 */
public class DeepDistributedExperimenter {

  private final static Logger L = Logger.getLogger(DeepDistributedExperimenter.class.getName());

  //java -cp EvolvedGrammaticalEvolution-1.0-SNAPSHOT.jar:. it.units.malelab.ege.DeepDistributedExperimenter hi 9000 diversities
  public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
    //prepare master
    String keyPhrase = args[0];
    int port = Integer.parseInt(args[1]);
    String baseResultDirName = args[2];
    String baseResultFileName = args[3];
    Master master = new Master(keyPhrase, port, baseResultDirName, baseResultFileName);
    master.start();
    List<Future<List<Node>>> results = new ArrayList<>();
    //prepare things
    int populationSize = 500;
    int generations = 50;
    int tournamentSize = 5;
    //define problems, methods, mappers
    int[] runs = new int[]{1, 2};
    List<String> problems = Lists.newArrayList(
            "bool-parity-5", "bool-parity-8", "bool-mopm-3",
            "sr-keijzer6", "sr-nguyen7", "sr-pagie1",
            "syn-klandscapes-7", "syn-text"
    );
    problems = problems.subList(0, 2);
    List<String> methods = Lists.newArrayList(
            "standard",
            "dc-g", "dc-p", "dc-f",
            "p-g-u-20", "p-p-a<-20", "p-p-a>-20", "p-p-u-20", "p-f-a<-20", "p-f-a>-20", "p-f-u-20", "p-f-l<-20", "p-f-l>-20");
    methods = methods.subList(0, 2);
    List<String> mappers = Lists.newArrayList(
            "ge-8-5-1024",
            "whge-3-1024",
            "sge-6",
            "cfggp-12");
    //prepare distances
    final Distance<Node<String>> phenotypeDistance = new CachedDistance<>(new LeavesEdit<>());
    Distance<Sequence<Boolean>> bitsGenotypeDistance = new CachedDistance<>(new Hamming<>());
    Distance<Sequence<Integer>> sgeGenotypeDistance = new CachedDistance<>(new Hamming<>());
    final Distance fitnessDistance = (Distance<NumericFitness>) (f1, f2) -> Math.abs(f1.getValue() - f2.getValue());
    //iterate
    for (int run : runs) {
      for (String pr : problems) {
        for (String me : methods) {
          for (String ma : mappers) {
            Map<String, Object> keys = new LinkedHashMap<>();
            keys.put("problem", pr);
            keys.put("mapping", ma);
            keys.put("method", me);
            keys.put(Master.RANDOM_SEED_NAME, run);
            //build problem
            Problem<String, NumericFitness> problem = null;
            switch (p(pr, 1)) {
              case "parity":
                problem = new Parity(i(p(pr, 2)));
                break;
              case "mopm":
                problem = new MultipleOutputParallelMultiplier(i(p(pr, 2)));
                break;
              case "keijzer6":
                problem = new HarmonicCurve();
                break;
              case "nguyen7":
                problem = new Nguyen7(0);
                break;
              case "pagie1":
                problem = new Pagie1();
                break;
              case "klandscapes":
                problem = new KLandscapes(i(p(pr, 2)));
                break;
              case "text":
                problem = new Text();
                break;
            }
            StandardConfiguration configuration = null;
            //build mapper, operators, initializer, genotype distance
            Mapper mapper = null;
            Map operators = new Utils.MapBuilder<>()
                    .put(new LengthPreservingTwoPointsCrossover(), 0.8d)
                    .put(new ProbabilisticMutation(0.01), 0.2d).build();
            PopulationInitializer populationInitializer = null;
            Distance genotypeDistance = bitsGenotypeDistance;
            switch (p(ma, 0)) {
              case "ge":
                mapper = new StandardGEMapper(i(p(ma, 1)), i(p(ma, 2)), problem.getGrammar());
                populationInitializer = new RandomInitializer<>(new BitsGenotypeFactory(i(p(ma, 3))));
                break;
              case "pige":
                mapper = new PiGEMapper(i(p(ma, 1)), i(p(ma, 2)), problem.getGrammar());
                populationInitializer = new RandomInitializer<>(new BitsGenotypeFactory(i(p(ma, 3))));
                break;
              case "hge":
                mapper = new HierarchicalMapper(problem.getGrammar());
                populationInitializer = new RandomInitializer<>(new BitsGenotypeFactory(i(p(ma, 1))));
                break;
              case "whge":
                mapper = new WeightedHierarchicalMapper(i(p(ma, 1)), problem.getGrammar());
                populationInitializer = new RandomInitializer<>(new BitsGenotypeFactory(i(p(ma, 2))));
                break;
              case "bitsge":
                mapper = new BitsSGEMapper(i(p(ma, 1)), problem.getGrammar());
                populationInitializer = new RandomInitializer<>(new BitsGenotypeFactory(i(p(ma, 2))));
                break;
              case "sge":
                mapper = new SGEMapper(i(p(ma, 1)), problem.getGrammar());
                operators = new Utils.MapBuilder<>()
                        .put(new SGECrossover<>(), 0.8d)
                        .put(new SGEMutation<>(0.01, (SGEMapper<String>) mapper), 0.2d).build();
                populationInitializer = new RandomInitializer<>(new SGEGenotypeFactory<>((SGEMapper<String>) mapper));
                genotypeDistance = sgeGenotypeDistance;
                break;
              case "cfggp":
                mapper = new CfgGpMapper();
                operators = new Utils.MapBuilder<>()
                        .put(new StandardTreeCrossover<>(i(p(ma, 1))), 0.8d)
                        .put(new StandardTreeMutation<>(i(p(ma, 1)), problem.getGrammar()), 0.2d).build();
                populationInitializer = new MultiInitializer<>(new Utils.MapBuilder<PopulationInitializer<Node<String>>, Double>()
                        .put(new RandomInitializer<>(new GrowTreeFactory<>(i(p(ma, 1)), problem.getGrammar())), 0.5)
                        .put(new RandomInitializer<>(new FullTreeFactory<>(i(p(ma, 1)), problem.getGrammar())), 0.5)
                        .build());
                genotypeDistance = phenotypeDistance;
                break;
            }
            //build configuration            
            switch (p(me, 0)) {
              case "standard":
                configuration = new StandardConfiguration(
                        populationSize, generations,
                        populationInitializer,
                        new Any(),
                        mapper,
                        operators,
                        new ComparableRanker(new IndividualComparator(IndividualComparator.Attribute.FITNESS)),
                        new Tournament(tournamentSize),
                        new LastWorst(),
                        1,
                        true,
                        problem,
                        false,
                        -1, -1
                );
                break;
              case "dc":
                final Distance localGenotypeDistance = genotypeDistance;
                Distance distance = null;
                switch (p(me, 1)) {
                  case "g":
                    distance = (Distance<Individual>) (i1, i2) -> localGenotypeDistance.d(i1.genotype, i2.genotype);
                    break;
                  case "p":
                    distance = (Distance<Individual>) (i1, i2) -> phenotypeDistance.d(i1.phenotype, i2.phenotype);
                    break;
                  case "f":
                    distance = (Distance<Individual>) (i1, i2) -> fitnessDistance.d(i1.fitness, i2.fitness);
                    break;
                }
                configuration = new DeterministicCrowdingConfiguration(
                        distance,
                        populationSize,
                        generations,
                        populationInitializer,
                        new Any(),
                        mapper,
                        operators,
                        new ComparableRanker(new IndividualComparator(IndividualComparator.Attribute.FITNESS)),
                        new Tournament(tournamentSize),
                        problem,
                        false,
                        -1, -1
                );
                break;
              case "p":
                Ranker<Individual> parentInPartitionRanker = null;
                Comparator<Individual> partitionerComparator = null;
                switch (p(me, 1)) {
                  case "g":
                    partitionerComparator = new IndividualComparator(IndividualComparator.Attribute.GENO);
                    break;
                  case "p":
                    partitionerComparator = new IndividualComparator(IndividualComparator.Attribute.PHENO);
                    break;
                  case "f":
                    partitionerComparator = new IndividualComparator(IndividualComparator.Attribute.FITNESS);
                    break;
                }
                switch (p(me, 2)) {
                  case "u":
                    parentInPartitionRanker = new RandomizerRanker();
                    break;
                  case "a<":
                    parentInPartitionRanker = new ComparableRanker(new IndividualComparator(IndividualComparator.Attribute.AGE));
                    break;
                  case "a>":
                    parentInPartitionRanker = new ComparableRanker(Collections.reverseOrder(new IndividualComparator(IndividualComparator.Attribute.AGE)));
                    break;
                  case "l<":
                    parentInPartitionRanker = new ComparableRanker(new IndividualComparator(IndividualComparator.Attribute.PHENO_SIZE));
                    break;
                  case "l>":
                    parentInPartitionRanker = new ComparableRanker(Collections.reverseOrder(new IndividualComparator(IndividualComparator.Attribute.PHENO_SIZE)));
                    break;
                }
                configuration = new PartitionConfiguration(
                        partitionerComparator,
                        i(p(me, 3)),
                        parentInPartitionRanker,
                        new FirstBest(),
                        parentInPartitionRanker,
                        new LastWorst(),
                        populationSize,
                        generations,
                        populationInitializer,
                        new Any(),
                        mapper,
                        operators,
                        new ComparableRanker(new IndividualComparator(IndividualComparator.Attribute.FITNESS)),
                        new Tournament(tournamentSize),
                        new LastWorst(),
                        1,
                        true,
                        problem,
                        false,
                        -1, -1
                );
                break;
            }
            Job job = new Job(
                    configuration,
                    Arrays.asList(new Population(),
                            new NumericFirstBest(false, problem.getTestingFitnessComputer(), "%6.2f"),
                            new Diversity(),
                            new BestPrinter(problem.getPhenotypePrinter(), "%30.30s")),
                    keys,
                    configuration.getOffspringSize(),
                    false
            );
            L.info(String.format("Submitting job: %s%n", job));
            results.add(master.submit(job));
          }
        }
      }
    }
    L.info(String.format("%d job submitted.%n", results.size()));
    for (Future<List<Node>> result : results) {
      L.info(String.format("Got %d solutions%n", result.get().size()));
    }
  }

  private static String p(String s, int n) {
    String[] pieces = s.split("-");
    if (n < pieces.length) {
      return pieces[n];
    }
    return null;
  }

  private static int i(String s) {
    return Integer.parseInt(s);
  }

}
