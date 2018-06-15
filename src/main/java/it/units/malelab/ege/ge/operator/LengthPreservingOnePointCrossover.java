/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.ge.operator;

import it.units.malelab.ege.core.operator.AbstractCrossover;
import it.units.malelab.ege.ge.genotype.BitsGenotype;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author eric
 */
public class LengthPreservingOnePointCrossover extends AbstractCrossover<BitsGenotype> {

  @Override
  public List<BitsGenotype> apply(List<BitsGenotype> parents, Random random) {
    BitsGenotype parent1 = parents.get(0);
    BitsGenotype parent2 = parents.get(1);
    int cutPointIndex1 = Math.min(Math.max(1, random.nextInt(parent1.leaves())), parent1.leaves()-1);
    int cutPointIndex2 = parent2.leaves()-(parent1.leaves()-cutPointIndex1);
    int child1Size = cutPointIndex1+(parent2.leaves()-cutPointIndex2);
    int child2Size = cutPointIndex2+(parent1.leaves()-cutPointIndex1);
    BitsGenotype child1 = new BitsGenotype(child1Size);
    BitsGenotype child2 = new BitsGenotype(child2Size);
    child1.set(0, parent1.slice(0, cutPointIndex1));
    child2.set(0, parent2.slice(0, cutPointIndex2));
    child1.set(cutPointIndex1, parent2.slice(cutPointIndex2, parent2.leaves()));
    child2.set(cutPointIndex2, parent1.slice(cutPointIndex1, parent1.leaves()));
    List<BitsGenotype> children = new ArrayList<>();
    children.add(child1);
    children.add(child2);
    return children;
  }
  
}
