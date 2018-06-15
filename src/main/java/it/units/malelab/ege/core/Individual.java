/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core;

import it.units.malelab.ege.core.fitness.Fitness;

import java.util.*;

/**
 *
 * @author eric
 */
public class Individual<G, T, F extends Fitness> {

  public final G genotype;
  public final Node<T> phenotype;
  public final F fitness;
  public final int birthDate;
  public final List<Individual<G, T, F>> parents;
  public final Map<String, Object> otherInfo;

  public Individual(G genotype, Node<T> phenotype, F fitness, int birthDate, Collection<Individual<G, T, F>> parents, Map<String, Object> otherInfo) {
    this.genotype = genotype;
    this.phenotype = phenotype;
    this.fitness = fitness;
    this.birthDate = birthDate;
    this.parents = new ArrayList<>();
    if (parents != null) {
      this.parents.addAll(parents);
    }
    this.otherInfo = new LinkedHashMap<>();
    if (otherInfo != null) {
      this.otherInfo.putAll(otherInfo);
    }
  }

  @Override
  public String toString() {
    return "Individual{" + "genotype=" + genotype + ", phenotype=" + phenotype + ", fitness=" + fitness + '}';
  }

  @Override
  public int hashCode() {
//    int hash = 5;
//    hash = 59 * hash + Objects.hashCode(this.genotype);
//    return hash;
    return genotype.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Individual<?, ?, ?> other = (Individual<?, ?, ?>) obj;
      return Objects.equals(this.genotype, other.genotype);
  }   

}
