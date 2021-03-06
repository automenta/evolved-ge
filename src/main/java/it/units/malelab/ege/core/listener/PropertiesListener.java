/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.listener;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.fitness.Fitness;
import it.units.malelab.ege.core.listener.collector.Collector;
import it.units.malelab.ege.core.listener.event.EvolutionEvent;
import it.units.malelab.ege.core.listener.event.EvolutionStartEvent;
import it.units.malelab.ege.core.listener.event.GenerationEvent;
import it.units.malelab.ege.core.listener.event.MappingEvent;
import it.units.malelab.ege.core.listener.event.OperatorApplicationEvent;
import it.units.malelab.ege.core.operator.GeneticOperator;
import it.units.malelab.ege.util.Pair;
import it.units.malelab.ege.util.Utils;
import it.units.malelab.ege.util.distance.Distance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author eric
 */
public class PropertiesListener<G, T, F extends Fitness> extends AbstractListener<G, T, F> implements Collector<G, T, F> {

  private final Map<String, List<Pair<Double, Double>>> partialDistances;
  private final Map<String, List<Pair<Double, Double>>> cumulativeDistances;
  private final Map<String, Multiset<CountType>> counts;

  private boolean resetData = false;

  private enum CountType {
    MAPPING, INVALID, OPERATOR_APPLICATION, REDUNDANT, BETTER_FITNESS, VALID_OPERATOR_APPLICATION
  }

  private final static String NO_OPERATOR = "_ALL";

  private final Comparator<F> fitnessComparator;
  private final Distance<G> genotypeDistance;
  private final Distance<Node<T>> phenotypeDistance;
  private final Map<Class<? extends GeneticOperator>, String> operatorNames;

  public PropertiesListener(
          Comparator<F> fitnessComparator,
          Distance<G> genotypeDistance,
          Distance<Node<T>> phenotypeDistance,
          Map<Class<? extends GeneticOperator>, String> operatorNames) {
    super(MappingEvent.class, OperatorApplicationEvent.class, EvolutionStartEvent.class);
    this.fitnessComparator = fitnessComparator;
    this.genotypeDistance = genotypeDistance;
    this.phenotypeDistance = phenotypeDistance;
    this.operatorNames = operatorNames;
    partialDistances = Collections.synchronizedMap(new LinkedHashMap<>());
    cumulativeDistances = Collections.synchronizedMap(new LinkedHashMap<>());
    counts = Collections.synchronizedMap(new LinkedHashMap<>());
    //reset data
    counts.put(NO_OPERATOR, (Multiset) ConcurrentHashMultiset.create());
    for (String operatorName : operatorNames.values()) {
      counts.put(operatorName, (Multiset) ConcurrentHashMultiset.create());
      partialDistances.put(operatorName, Collections.synchronizedList(new ArrayList<>()));
      cumulativeDistances.put(operatorName, Collections.synchronizedList(new ArrayList<>()));
    }
  }

  @Override
  public void listen(EvolutionEvent<G, T, F> event) {
    synchronized (this) {
      if (resetData) {
        resetData = false;
        //reset data
        counts.get(NO_OPERATOR).clear();
        for (String operatorName : operatorNames.values()) {
          counts.get(operatorName).clear();
          partialDistances.get(operatorName).clear();
        }
      }
    }
    if (event instanceof MappingEvent) {
      MappingEvent<G, T, F> mappingEvent = (MappingEvent<G, T, F>) event;
      counts.get(NO_OPERATOR).add(CountType.MAPPING);
      if (Node.EMPTY_TREE.equals(mappingEvent.getPhenotype())) {
        counts.get(NO_OPERATOR).add(CountType.INVALID);
      }
    }
    if (event instanceof OperatorApplicationEvent) {
      OperatorApplicationEvent<G, T, F> operatorApplicationEvent = (OperatorApplicationEvent<G, T, F>) event;
      String operatorName = getOperatorName(operatorApplicationEvent.getOperator());
      if (operatorName != null) {
        counts.get(operatorName).add(CountType.OPERATOR_APPLICATION);
        boolean differentPhenotype = false;
        boolean differentGenotype = false;
        boolean allInvalid = true;
        //compute distances
        for (Individual<G, T, F> child : operatorApplicationEvent.children) {
          if (Node.EMPTY_TREE.equals(child.phenotype)) {
            continue;
          }
          double minGenotypeDistance = Double.POSITIVE_INFINITY;
          double minPhenotypeDistance = Double.POSITIVE_INFINITY;
          for (Individual<G, T, F> parent : operatorApplicationEvent.getParents()) {
            if (Node.EMPTY_TREE.equals(parent.phenotype)) {
              continue;
            }
            allInvalid = false;
            double localGenotypeDistance = genotypeDistance.d(parent.genotype, child.genotype);
            if (minGenotypeDistance > localGenotypeDistance) {
              minGenotypeDistance = localGenotypeDistance;
            }
            double localPhenotypeDistance = phenotypeDistance.d(parent.phenotype, child.phenotype);
            if (minPhenotypeDistance > localPhenotypeDistance) {
              minPhenotypeDistance = localPhenotypeDistance;
            }
          }
          //add sample for locality
          if (minPhenotypeDistance < Double.POSITIVE_INFINITY) {
            partialDistances.get(operatorName).add(new Pair<>(minGenotypeDistance, minPhenotypeDistance));
            cumulativeDistances.get(operatorName).add(new Pair<>(minGenotypeDistance, minPhenotypeDistance));
          }
          //check difference
          differentGenotype = differentGenotype || (minGenotypeDistance > 0);
          differentPhenotype = differentPhenotype || (minPhenotypeDistance > 0);
        }
        //check redundancy
        if (!allInvalid) {
          counts.get(operatorName).add(CountType.VALID_OPERATOR_APPLICATION);
          if (differentGenotype && !differentPhenotype) {
            counts.get(operatorName).add(CountType.REDUNDANT);
          }
        }
        //compute best fitnesses
        F bestParentFitness = null;
        for (Individual<G, T, F> parent : operatorApplicationEvent.getParents()) {
            if ((bestParentFitness == null) || (fitnessComparator.compare(parent.fitness, bestParentFitness) < 0)) {
                bestParentFitness = parent.fitness;
          }
        }
        F bestChildFitness = null;
        for (Individual<G, T, F> child : operatorApplicationEvent.children) {
            if ((bestChildFitness == null) || (fitnessComparator.compare(child.fitness, bestChildFitness) < 0)) {
                bestChildFitness = child.fitness;
          }
        }
        //check evolvability
        if (fitnessComparator.compare(bestChildFitness, bestParentFitness) < 0) {
          counts.get(operatorName).add(CountType.BETTER_FITNESS);
        }
      }
    }
  }

  private String getOperatorName(GeneticOperator<G> geneticOperator) {
    for (Map.Entry<Class<? extends GeneticOperator>, String> entry : operatorNames.entrySet()) {
      if (entry.getKey().isAssignableFrom(geneticOperator.getClass())) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Override
  public Map<String, String> getFormattedNames() {
    Map<String, String> formattedNames = new LinkedHashMap<>();
    formattedNames.put("properties.mapping.invalidity", "%4.2f");
    formattedNames.put("properties.mapping.count", "%4d");
    for (String operatorName : operatorNames.values()) {
      formattedNames.put("properties.operator." + operatorName + ".redundancy", "%4.2f");
      formattedNames.put("properties.operator." + operatorName + ".locality", "%+4.2f");
      formattedNames.put("properties.operator." + operatorName + ".locality.cumulative", "%+4.2f");
      formattedNames.put("properties.operator." + operatorName + ".evolvability", "%4.2f");
      formattedNames.put("properties.operator." + operatorName + ".count", "%4d");
      formattedNames.put("properties.operator." + operatorName + ".count.valid", "%4d");
    }
    return formattedNames;
  }

  @Override
  public Map<String, Object> collect(GenerationEvent<G, T, F> generationEvent) {
    //compute aggregates
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("properties.mapping.invalidity", (double) counts.get(NO_OPERATOR).count(CountType.INVALID) / counts.get(NO_OPERATOR).count(CountType.MAPPING));
    values.put("properties.mapping.count", counts.get(NO_OPERATOR).count(CountType.MAPPING));
    for (String operatorName : operatorNames.values()) {
      values.put("properties.operator." + operatorName + ".redundancy",
              (double) counts.get(operatorName).count(CountType.REDUNDANT) / counts.get(operatorName).count(CountType.VALID_OPERATOR_APPLICATION)
      );
      values.put("properties.operator." + operatorName + ".locality",
              Utils.pearsonCorrelation(partialDistances.get(operatorName))
      );
      values.put("properties.operator." + operatorName + ".locality.cumulative",
              Utils.pearsonCorrelation(cumulativeDistances.get(operatorName))
      );
      values.put("properties.operator." + operatorName + ".evolvability",
              (double) counts.get(operatorName).count(CountType.BETTER_FITNESS) / counts.get(operatorName).count(CountType.OPERATOR_APPLICATION)
      );
      values.put("properties.operator." + operatorName + ".count",
              counts.get(operatorName).count(CountType.OPERATOR_APPLICATION)
      );
      values.put("properties.operator." + operatorName + ".count.valid",
              counts.get(operatorName).count(CountType.VALID_OPERATOR_APPLICATION)
      );
    }
    return values;
  }

}
