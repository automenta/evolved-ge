/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.listener.collector;

import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.Sequence;
import it.units.malelab.ege.core.fitness.Fitness;
import it.units.malelab.ege.core.fitness.FitnessComputer;
import it.units.malelab.ege.core.listener.event.GenerationEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterables.size;

/**
 * @author eric
 */
public abstract class Best<G extends Sequence, T, F extends Fitness> implements Collector<G, T, F> {

    private final boolean ancestry;
    private final FitnessComputer<T, F> validationFitnessComputer;

    Best(boolean ancestry, FitnessComputer<T, F> validationFitnessComputer) {
        this.ancestry = ancestry;
        this.validationFitnessComputer = validationFitnessComputer;
    }

    private static String augmentFitnessName(String prefix, String fitnessName) {
        if (fitnessName.isEmpty()) {
            return prefix;
        }
        return prefix + '.' + fitnessName;
    }

    @Override
    public Map<String, Object> collect(GenerationEvent<G, T, F> event) {
        List<List<Individual<G, T, F>>> rankedPopulation = new ArrayList<>(event.getRankedPopulation());
        Individual<G, T, F> best = rankedPopulation.get(0).get(0);

        Map<String, Object> f = getFitnessIndexes(best.fitness);
        Map<String, Object> indexes = new LinkedHashMap<>(f.size());
        for (Map.Entry<String, Object> fitnessEntry: f.entrySet()) {
            indexes.put(
                    augmentFitnessName("best.fitness", fitnessEntry.getKey()),
                    fitnessEntry.getValue());
        }
        if (validationFitnessComputer != null) {
            F validationFitness = validationFitnessComputer.compute(best.phenotype);
            for (Map.Entry<String, Object> fitnessEntry: getFitnessIndexes(validationFitness).entrySet()) {
                indexes.put(
                        augmentFitnessName("best.validation.fitness", fitnessEntry.getKey()),
                        fitnessEntry.getValue());
            }
        }
        indexes.put("best.genotype.size", best.genotype.size());
        indexes.put("best.phenotype.size", size(best.phenotype.leafNodes()));
        indexes.put("best.phenotype.nodeSize", best.phenotype.nodeSize());
        indexes.put("best.phenotype.depth", best.phenotype.depth());
        indexes.put("best.birth", best.birthDate);
        if (ancestry) {
            indexes.put("best.ancestry.depth", getAncestryDepth(best));
            indexes.put("best.ancestry.size", getAncestrySize(best));
        }
        return indexes;
    }

    @Override
    public Map<String, String> getFormattedNames() {

        Map<String, String> f = getFitnessFormattedNames();
        Map<String, String> formattedNames = new LinkedHashMap<>(f.size()*2);
        for (Map.Entry<String, String> fitnessEntry: f.entrySet()) {
            formattedNames.put(
                    augmentFitnessName("best.fitness", fitnessEntry.getKey()),
                    fitnessEntry.getValue());
        }
        for (Map.Entry<String, String> fitnessEntry: f.entrySet()) {
            formattedNames.put(
                    augmentFitnessName("best.validation.fitness", fitnessEntry.getKey()),
                    fitnessEntry.getValue());
        }
        formattedNames.put("best.genotype.size", "%4d");
        formattedNames.put("best.phenotype.size", "%4d");
        formattedNames.put("best.phenotype.nodeSize", "%5d");
        formattedNames.put("best.phenotype.depth", "%2d");
        formattedNames.put("best.birth", "%3d");
        if (ancestry) {
            formattedNames.put("best.ancestry.depth", "%2d");
            formattedNames.put("best.ancestry.size", "%5d");
        }
        return formattedNames;
    }

    private int getAncestrySize(Individual<G, T, F> individual) {
        int count = 1;
        for (Individual<G, T, F> parent: individual.parents) {
            count = count + getAncestrySize(parent);
        }
        return count;
    }

    private int getAncestryDepth(Individual<G, T, F> individual) {
        int count = 1;
        for (Individual<G, T, F> parent: individual.parents) {
            count = Math.max(count, getAncestryDepth(parent) + 1);
        }
        return count;
    }

    protected abstract Map<String, String> getFitnessFormattedNames();

    protected abstract Map<String, Object> getFitnessIndexes(F fitness);


}
