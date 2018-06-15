/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.selector;

import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.Sequence;
import it.units.malelab.ege.core.fitness.Fitness;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.collect.Iterables.size;

/**
 * @author eric
 */
public class IndividualComparator<G, T, F extends Fitness> implements Comparator<Individual<G, T, F>>, Serializable {

    private final Map<Attribute, Boolean> attributes;

    public IndividualComparator(Map<Attribute, Boolean> attributes) {
        this.attributes = attributes;
    }

    public IndividualComparator(Attribute... attributes) {
        this.attributes = new LinkedHashMap<>();
        for (Attribute attribute: attributes) {
            this.attributes.put(attribute, false);
        }
    }

    @Override
    public int compare(Individual<G, T, F> i1, Individual<G, T, F> i2) {
        if (i1 == i2) return 0;
        int v = -1;
        for (Map.Entry<Attribute, Boolean> entry: attributes.entrySet()) {
            Attribute key = entry.getKey();
            if (key.equals(Attribute.FITNESS)) {
                if (i1.fitness instanceof Comparable) {
                    v = ((Comparable) i1.fitness).compareTo(i2.fitness);
                } else {
                    v = 0;
                }
            } else if (key.equals(Attribute.AGE)) {
                v = -Integer.compare(i1.birthDate, i2.birthDate);
            } else if (key.equals(Attribute.PHENO_SIZE)) {
                v = Integer.compare(size(i1.phenotype.leafNodes()), size(i2.phenotype.leafNodes()));
            } else if (key.equals(Attribute.GENO_SIZE)) {
                if (i1.genotype instanceof Sequence) {
                    v = Integer.compare(((Sequence) i1.genotype).leaves(), ((Sequence) i2.genotype).leaves());
                } else {
                    v = 0;
                }
            } else if (key.equals(Attribute.GENO)) {
                v = (i1.genotype.equals(i2.genotype)) ? 0 : 1;
            } else if (key.equals(Attribute.PHENO)) {
                v = (i1.phenotype.equals(i2.phenotype)) ? 0 : 1;
            }
            if (entry.getValue()) {
                v = -v;
            }
            if (v != 0) {
                break;
            }
        }
        return v;
    }

    @Override
    public String toString() {
        return "IndividualComparator{" + "attributes=" + attributes + '}';
    }

    public enum Attribute {
        FITNESS, AGE, PHENO_SIZE, GENO_SIZE, PHENO, GENO
    }

}
