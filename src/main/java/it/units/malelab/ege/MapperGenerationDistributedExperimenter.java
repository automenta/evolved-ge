/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege;

import com.google.common.base.Joiner;
import it.units.malelab.ege.benchmark.KLandscapes;
import it.units.malelab.ege.benchmark.Text;
import it.units.malelab.ege.benchmark.mapper.MapperGeneration;
import it.units.malelab.ege.benchmark.mapper.MappingPropertiesFitness;
import it.units.malelab.ege.benchmark.mapper.RecursiveMapper;
import it.units.malelab.ege.benchmark.symbolicregression.Pagie1;
import it.units.malelab.ege.cfggp.initializer.FullTreeFactory;
import it.units.malelab.ege.cfggp.initializer.GrowTreeFactory;
import it.units.malelab.ege.cfggp.mapper.CfgGpMapper;
import it.units.malelab.ege.cfggp.operator.StandardTreeCrossover;
import it.units.malelab.ege.cfggp.operator.StandardTreeMutation;
import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.Problem;
import it.units.malelab.ege.core.evolver.DeterministicCrowdingConfiguration;
import it.units.malelab.ege.core.evolver.PartitionConfiguration;
import it.units.malelab.ege.core.evolver.StandardConfiguration;
import it.units.malelab.ege.core.fitness.FitnessComputer;
import it.units.malelab.ege.core.fitness.MultiObjectiveFitness;
import it.units.malelab.ege.core.fitness.NumericFitness;
import it.units.malelab.ege.core.initializer.MultiInitializer;
import it.units.malelab.ege.core.initializer.PopulationInitializer;
import it.units.malelab.ege.core.initializer.RandomInitializer;
import it.units.malelab.ege.core.listener.collector.BestPrinter;
import it.units.malelab.ege.core.listener.collector.Collector;
import it.units.malelab.ege.core.listener.collector.Diversity;
import it.units.malelab.ege.core.listener.collector.MultiObjectiveFitnessFirstBest;
import it.units.malelab.ege.core.listener.collector.NumericFirstBest;
import it.units.malelab.ege.core.listener.collector.Population;
import it.units.malelab.ege.core.operator.GeneticOperator;
import it.units.malelab.ege.core.ranker.ComparableRanker;
import it.units.malelab.ege.core.ranker.ParetoRanker;
import it.units.malelab.ege.core.selector.FirstBest;
import it.units.malelab.ege.core.selector.IndividualComparator;
import it.units.malelab.ege.core.selector.LastWorst;
import it.units.malelab.ege.core.selector.Tournament;
import it.units.malelab.ege.ge.genotype.BitsGenotype;
import it.units.malelab.ege.ge.genotype.BitsGenotypeFactory;
import it.units.malelab.ege.core.validator.Any;
import it.units.malelab.ege.distributed.DistributedUtils;
import it.units.malelab.ege.distributed.Job;
import it.units.malelab.ege.distributed.master.Master;
import it.units.malelab.ege.ge.operator.LengthPreservingTwoPointsCrossover;
import it.units.malelab.ege.ge.operator.ProbabilisticMutation;
import it.units.malelab.ege.util.Pair;
import it.units.malelab.ege.util.Utils;
import static it.units.malelab.ege.util.Utils.*;
import it.units.malelab.ege.util.distance.CachedDistance;
import it.units.malelab.ege.util.distance.Distance;
import it.units.malelab.ege.util.distance.LeavesEdit;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 *
 * @author eric
 */
public class MapperGenerationDistributedExperimenter {

  private final static Logger L = Logger.getLogger(MapperGenerationDistributedExperimenter.class.getName());

  private static final int expressivenessDepth = 2;
  private static final int nGenotypes = 100;
  static final int nOfFirstRank = 5;
  private static final int learningMaxDepth = 14;
  private static final int learningPopSize = 500;
  private static final int learningRuns = 10;
  private static final int learningMaxPartitionSize = 10;
  private static final int learningGenerations = 50;
  private static final int learningGenotypeSize = 256;
  private static final int learningMaxMappingDepth = 9;
  private static final int validationGenotypeSize = 1024;
  private static final int validationMaxMappingDepth = 9;
  private static final int validationGenerations = 30;
  private static final int validationPopSize = 500;
  private static final int validationRuns = 5;
  static final boolean validateMappers = false;
  private final Map<String, Problem<String, NumericFitness>> validationProblems = new LinkedHashMap<>();
  private final List<Problem<String, NumericFitness>> learningProblems = new ArrayList<>();
  final Map<String, Node<String>> baselines = new LinkedHashMap<>();
  private final FitnessComputer<String, MultiObjectiveFitness<Double>> baseMOF;
  final Master master;

  final String baseResultFileName;

  private static final String FITNESS_NAME = "fitness";

  MapperGenerationDistributedExperimenter(String keyPhrase, int port, String baseResultDirName, String baseResultFileName) throws IOException {
    this.baseResultFileName = baseResultFileName;
    validationProblems.put("Pagie1", new Pagie1());
    validationProblems.put("KLand-5", new KLandscapes(5));
    validationProblems.put("Text", new Text());
    learningProblems.add(new Pagie1());
    baselines.put("GE", getGERawTree());
    baselines.put("HGE", getHGERawTree());
    baselines.put("WHGE", getWHGERawTree());
    baseMOF = new MappingPropertiesFitness(
            validationGenotypeSize,
            nGenotypes,
            validationMaxMappingDepth,
            new Random(1l),
            learningProblems,
            MappingPropertiesFitness.Property.REDUNDANCY,
            MappingPropertiesFitness.Property.NON_LOCALITY,
            MappingPropertiesFitness.Property.NON_UNIFORMITY);
    master = new Master(keyPhrase, port, baseResultDirName, baseResultFileName);
    master.start();
  }

  public static void main(String[] args) throws IOException {
    String keyPhrase = args[0];
    int port = Integer.parseInt(args[1]);
    String baseResultDirName = args[2];
    String baseResultFileName = args[3];
    MapperGenerationDistributedExperimenter experimenter = new MapperGenerationDistributedExperimenter(keyPhrase, port, baseResultDirName, baseResultFileName);
    experimenter.start();
  }

  void start() throws IOException {
    //fitnessis
    MappingPropertiesFitness.Property[][] propertiesArrays = new MappingPropertiesFitness.Property[][]{
      new MappingPropertiesFitness.Property[]{MappingPropertiesFitness.Property.REDUNDANCY},
      new MappingPropertiesFitness.Property[]{MappingPropertiesFitness.Property.REDUNDANCY, MappingPropertiesFitness.Property.NON_LOCALITY},
      new MappingPropertiesFitness.Property[]{MappingPropertiesFitness.Property.REDUNDANCY, MappingPropertiesFitness.Property.NON_LOCALITY, MappingPropertiesFitness.Property.NON_UNIFORMITY},};
    //baseline jobs
    for (Map.Entry<String, Node<String>> baselineEntry : baselines.entrySet()) {
      String mapperName = baselineEntry.getKey();
      Node<String> mapper = baselineEntry.getValue();
      if (validateMappers) {
        submitValidationJobs(mapper, mapperName, 0, 0);
      }
    }
    //results (futures)
    Map<Job, Future<List<Node>>> resultsMap = new HashMap<>();
    for (MappingPropertiesFitness.Property[] properties : propertiesArrays) {
      Random random = new Random(1l);
      Problem<String, MultiObjectiveFitness<Double>> problem = new MapperGeneration(
              learningGenotypeSize,
              nGenotypes,
              learningMaxMappingDepth,
              random,
              learningProblems,
              properties
      );
      String[] propertyString = new String[properties.length];
      for (int i = 0; i < properties.length; i++) {
        propertyString[i] = properties[i].getShortName();
      }
      String propertiesString = Joiner.on("/").join(propertyString);
      List<Collector<Node<String>, String, MultiObjectiveFitness<Double>>> collectors = Arrays.asList(
              new Population<>(),
              new MultiObjectiveFitnessFirstBest<>(false, problem.getTestingFitnessComputer(), "%4.2f", "%4.2f", "%4.2f"),
              new Diversity<>(),
              new BestPrinter<>(problem.getPhenotypePrinter(), "%40.40s"));
      for (int r = 0; r < learningRuns; r++) {
        Job job = buildLearningJobPartition(problem, propertiesString, r, (List) collectors);
        resultsMap.put(job, master.submit(job));
      }
    }
    //wait for results
    Pair<Job, List<Node>> resultsPair;
    while ((resultsPair = DistributedUtils.getAny(resultsMap, 250)) != null) {
        Job job = resultsPair.first;
        List<Node> results = resultsPair.second;
      int initialSize = results.size();
      Map<Node<String>, MultiObjectiveFitness<Double>> firstRank = new LinkedHashMap<>();
      for (int i = 0; i < results.size(); i++) {
        Node<String> mapper = (Node<String>) results.get(i);
        String mapperName = "generated-" + job.getKeys().get(FITNESS_NAME);
        MultiObjectiveFitness<Double> mof = saveMapper(mapper, mapperName, (Integer) job.getKeys().get(Master.RANDOM_SEED_NAME), i);
        firstRank.put(mapper, mof);
      }
      results.clear();
      results.addAll(Utils.maximallySparseSubset(firstRank, nOfFirstRank));
      L.info(String.format("Results of job %s reduced from %d to %d.", job, initialSize, results.size()));
      if (validateMappers) {
        for (int i = 0; i < results.size(); i++) {
          Node<String> mapper = (Node<String>) results.get(i);
          String mapperName = "generated-" + job.getKeys().get(FITNESS_NAME);
          submitValidationJobs(mapper, mapperName, (Integer) job.getKeys().get(Master.RANDOM_SEED_NAME), i);
          saveMapper(mapper, mapperName, (Integer) job.getKeys().get(Master.RANDOM_SEED_NAME), i);
        }
      }
    }
  }

  void submitValidationJobs(Node<String> mapper, String mapperName, int outerRun, int inRankIndex) {
    L.info(String.format("Submitting validation jobs for mapper %s:%d:%d", mapperName, outerRun, inRankIndex));
    for (int r = 0; r < validationRuns; r++) {
      for (Map.Entry<String, Problem<String, NumericFitness>> problemEntry : validationProblems.entrySet()) {
        List<Collector<BitsGenotype, String, NumericFitness>> collectors = Arrays.asList(
                new Population<>(),
                new NumericFirstBest<>(false, problemEntry.getValue().getTestingFitnessComputer(), "%6.2f"),
                new Diversity<>(),
                new BestPrinter<>(problemEntry.getValue().getPhenotypePrinter(), "%40.40s"));
        master.submit(buildValidationJob(mapper, mapperName, outerRun, inRankIndex, problemEntry.getValue(), problemEntry.getKey(), r, (List) collectors));
      }
    }
  }

  private MultiObjectiveFitness<Double> saveMapper(Node<String> mapper, String mapperName, int outerRun, int inRankIndex) {
    //save mapper and its properties
    MultiObjectiveFitness<Double> mof = baseMOF.compute(mapper);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("mapper.name", mapperName);
    data.put("mapper", mapper);
    data.put("mapper.serialized.base64", serializeBase64(mapper));
    data.put("outer.run", outerRun);
    data.put("in.rank.index", inRankIndex);
    data.put("redundancy", mof.getValue()[0]);
    data.put("non.locality", mof.getValue()[1]);
    data.put("non.uniformity", mof.getValue()[2]);
    List<String> keys = new ArrayList<>(data.keySet());
    PrintStream mappersPs = master.getPrintStreamFactory().get(keys, baseResultFileName + ".mappers");
    for (int i = 0; i < keys.size(); i++) {
      mappersPs.print(data.get(keys.get(i)));
      if (i != (keys.size() - 1)) {
        mappersPs.print(";");
      }
    }
    mappersPs.println();
    return mof;
  }

  private static String serializeBase64(Serializable o) {
    ObjectOutputStream oos = null;
    String s = null;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      oos = new ObjectOutputStream(baos);
      oos.writeObject(o);
      s = Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (IOException ex) {
      //ignore
    } finally {
      try {
        oos.close();
      } catch (IOException ex) {
        //ignore
      }
    }
    return s;
  }

  private static Node<String> getGERawTree() {
    return node("<mapper>",
            node("<n>",
                    node("<fun_n_g>",
                            node("int")
                    ),
                    node("("),
                    node("<g>",
                            node("<fun_g_g,n>",
                                    node("substring")
                            ),
                            node("("),
                            node("<g>",
                                    node("<fun_g_g,n>",
                                            node("rotate_sx")
                                    ),
                                    node("("),
                                    node("<g>",
                                            node("<var_g>",
                                                    node("g")
                                            )
                                    ),
                                    node(","),
                                    node("<n>",
                                            node("<fun_n_n,n>",
                                                    node("*")
                                            ),
                                            node("("),
                                            node("<n>",
                                                    node("<var_n>",
                                                            node("g_count_rw")
                                                    )),
                                            node(","),
                                            node("<n>",
                                                    node("<const_n>",
                                                            node("8")
                                                    )),
                                            node(")")
                                    ),
                                    node(")")
                            ),
                            node(","),
                            node("<n>",
                                    node("<const_n>",
                                            node("8")
                                    )
                            ),
                            node(")")
                    ),
                    node(")")
            ),
            node("<lg>",
                    node("<fun_lg_g,n>",
                            node("repeat")
                    ),
                    node("("),
                    node("<g>",
                            node("<var_g>",
                                    node("g")
                            )
                    ),
                    node(","),
                    node("<n>",
                            node("<fun_n_ln>",
                                    node("length")
                            ),
                            node("("),
                            node("<ln>",
                                    node("<var_ln>",
                                            node("ln")
                                    )
                            ),
                            node(")")
                    ),
                    node(")")
            )
    );
  }

  static Node<String> getWHGERawTree() {
    return node("<mapper>",
            node("<n>",
                    node("<fun_n_ln>",
                            node("max_index")
                    ),
                    node("("),
                    node("<ln>",
                            node("apply"),
                            node("("),
                            node("<fun_n_g>",
                                    node("weight_r")),
                            node(","),
                            node("<lg>",
                                    node("<fun_lg_g,n>",
                                            node("split")
                                    ),
                                    node("("),
                                    node("<g>",
                                            node("<var_g>",
                                                    node("g")
                                            )
                                    ),
                                    node(","),
                                    node("<n>",
                                            node("<fun_n_ln>",
                                                    node("length")
                                            ),
                                            node("("),
                                            node("<ln>",
                                                    node("<var_ln>",
                                                            node("ln")
                                                    )
                                            ),
                                            node(")")
                                    ),
                                    node(")")
                            ),
                            node(")")
                    ),
                    node(")")
            ),
            node("<lg>",
                    node("<fun_lg_g,ln>",
                            node("split_w")
                    ),
                    node("("),
                    node("<g>",
                            node("<var_g>",
                                    node("g")
                            )
                    ),
                    node(","),
                    node("<ln>",
                            node("<var_ln>",
                                    node("ln")
                            )
                    ),
                    node(")")
            )
    );
  }

  private static Node<String> getHGERawTree() {
    return node("<mapper>",
            node("<n>",
                    node("<fun_n_ln>",
                            node("max_index")
                    ),
                    node("("),
                    node("<ln>",
                            node("apply"),
                            node("("),
                            node("<fun_n_g>",
                                    node("weight_r")),
                            node(","),
                            node("<lg>",
                                    node("<fun_lg_g,n>",
                                            node("split")
                                    ),
                                    node("("),
                                    node("<g>",
                                            node("<var_g>",
                                                    node("g")
                                            )
                                    ),
                                    node(","),
                                    node("<n>",
                                            node("<fun_n_ln>",
                                                    node("length")
                                            ),
                                            node("("),
                                            node("<ln>",
                                                    node("<var_ln>",
                                                            node("ln")
                                                    )
                                            ),
                                            node(")")
                                    ),
                                    node(")")
                            ),
                            node(")")
                    ),
                    node(")")
            ),
            node("<lg>",
                    node("<fun_lg_g,n>",
                            node("split")
                    ),
                    node("("),
                    node("<g>",
                            node("<var_g>",
                                    node("g")
                            )
                    ),
                    node(","),
                    node("<n>",
                            node("<fun_n_ln>",
                                    node("length")
                            ),
                            node("("),
                            node("<ln>",
                                    node("<var_ln>",
                                            node("ln")
                                    )
                            ),
                            node(")")
                    ),
                    node(")")
            )
    );
  }

  private static Job buildValidationJob(
          Node<String> rawMapper, String mapperName, int outerRun, int inRankIndex,
          Problem<String, NumericFitness> problem, String problemName,
          int run,
          List<Collector> collectors) {
    StandardConfiguration<BitsGenotype, String, NumericFitness> configuration = new StandardConfiguration<>(
            validationPopSize,
            validationGenerations,
            new RandomInitializer<>(new BitsGenotypeFactory(validationGenotypeSize)),
            new Any<>(),
            new RecursiveMapper<>(rawMapper, validationMaxMappingDepth, expressivenessDepth, problem.getGrammar()),
            new Utils.MapBuilder<GeneticOperator<BitsGenotype>, Double>()
                    .put(new LengthPreservingTwoPointsCrossover(), 0.8d)
                    .put(new ProbabilisticMutation(0.01), 0.2d).build(),
            new ComparableRanker<>(new IndividualComparator<>(IndividualComparator.Attribute.FITNESS)),
            new Tournament<>(3),
            new LastWorst<>(),
            validationPopSize,
            true,
            problem,
            false,
            -1, -1
    );
    Map<String, Object> keys = new LinkedHashMap<>();
    keys.put("problem", problemName);
    keys.put("mapper", mapperName);
    keys.put("outer.run", outerRun);
    keys.put("in.rank.index", inRankIndex);
    keys.put(Master.RANDOM_SEED_NAME, run);
    return new Job(configuration, collectors, keys, validationPopSize, false);
  }

  private static Job buildLearningJobPartition(Problem<String, MultiObjectiveFitness<Double>> problem, String fitnessName, int run, List<Collector> collectors) {
    PartitionConfiguration<Node<String>, String, MultiObjectiveFitness<Double>> configuration = new PartitionConfiguration<>(
            new IndividualComparator<>(IndividualComparator.Attribute.PHENO),
            learningMaxPartitionSize,
            new ComparableRanker<>(new IndividualComparator<>(IndividualComparator.Attribute.AGE)),
            new FirstBest<>(),
            new ComparableRanker<>(new IndividualComparator<>(IndividualComparator.Attribute.AGE)),
            new LastWorst<>(),
            learningPopSize,
            learningGenerations,
            new MultiInitializer<>(new Utils.MapBuilder<PopulationInitializer<Node<String>>, Double>()
                    .put(new RandomInitializer<>(new GrowTreeFactory<>(learningMaxDepth, problem.getGrammar())), 0.5)
                    .put(new RandomInitializer<>(new FullTreeFactory<>(learningMaxDepth, problem.getGrammar())), 0.5)
                    .build()
            ),
            new Any<>(),
            new CfgGpMapper<>(),
            new Utils.MapBuilder<GeneticOperator<Node<String>>, Double>()
                    .put(new StandardTreeCrossover<>(learningMaxDepth), 0.8d)
                    .put(new StandardTreeMutation<>(learningMaxDepth, problem.getGrammar()), 0.2d)
                    .build(),
            new ParetoRanker<>(),
            new Tournament<>(3),
            new LastWorst<>(),
            learningPopSize,
            true,
            problem,
            false,
            -1, -1
    );
    Map<String, Object> keys = new LinkedHashMap<>();
    keys.put(FITNESS_NAME, fitnessName);
    keys.put(Master.RANDOM_SEED_NAME, run);
    return new Job(configuration, collectors, keys, learningPopSize, true);
  }

  private static Job buildLearningJobDC(Problem<String, MultiObjectiveFitness<Double>> problem, String fitnessName, int run, List<Collector> collectors) {
    DeterministicCrowdingConfiguration<Node<String>, String, MultiObjectiveFitness<Double>> configuration = new DeterministicCrowdingConfiguration<>(
            new Distance<>() {
                private final Distance<Node<String>> phenotypeDistance = new CachedDistance<>(new LeavesEdit<String>());

                @Override
                public double d(Individual<Node<String>, String, MultiObjectiveFitness<Double>> i1, Individual<Node<String>, String, MultiObjectiveFitness<Double>> i2) {
                    return phenotypeDistance.d(i1.phenotype, i2.phenotype);
                }
            },
            learningPopSize,
            learningGenerations,
            new MultiInitializer<>(new Utils.MapBuilder<PopulationInitializer<Node<String>>, Double>()
                    .put(new RandomInitializer<>(new GrowTreeFactory<>(learningMaxDepth, problem.getGrammar())), 0.5)
                    .put(new RandomInitializer<>(new FullTreeFactory<>(learningMaxDepth, problem.getGrammar())), 0.5)
                    .build()
            ),
            new Any<>(), new CfgGpMapper<>(), new Utils.MapBuilder<GeneticOperator<Node<String>>, Double>()
                    .put(new StandardTreeCrossover<>(learningMaxDepth), 0.8d)
                    .put(new StandardTreeMutation<>(learningMaxDepth, problem.getGrammar()), 0.2d)
                    .build(),
            new ParetoRanker<>(),
            new Tournament<>(3),
            problem,
            false,
            -1, -1
    );
    Map<String, Object> keys = new LinkedHashMap<>();
    keys.put(FITNESS_NAME, fitnessName);
    keys.put(Master.RANDOM_SEED_NAME, run);
    return new Job(configuration, collectors, keys, 1, true);
  }

}
