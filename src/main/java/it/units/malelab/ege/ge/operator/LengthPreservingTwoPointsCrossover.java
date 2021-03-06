/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.ge.operator;

import com.google.common.collect.Range;
import it.units.malelab.ege.ge.genotype.BitsGenotype;

import java.util.List;
import java.util.Random;

/**
 *
 * @author eric
 */
public class LengthPreservingTwoPointsCrossover extends TwoPointsCrossover {

  @Override
  public List<BitsGenotype> apply(List<BitsGenotype> parents, Random random) {
    BitsGenotype parent1 = parents.get(0);
    BitsGenotype parent2 = parents.get(1);
    int startIndex1 = Math.min(Math.max(1, random.nextInt(parent1.leaves())), parent1.leaves()-2);
    int startIndex2 = Math.min(Math.max(1, random.nextInt(parent2.leaves())), parent2.leaves()-2);
    int crossoverSize = Math.max(1, random.nextInt(Math.min(parent1.leaves()-startIndex1, parent2.leaves()-startIndex2)));
    int endIndex1 = startIndex1+crossoverSize;
    int endIndex2 = startIndex2+crossoverSize;
    return children(
            parent1, Range.closedOpen(startIndex1, endIndex1),
            parent2, Range.closedOpen(startIndex2, endIndex2));
  }
  
}
