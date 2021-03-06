/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.ge.operator;

import it.units.malelab.ege.core.operator.AbstractMutation;
import it.units.malelab.ege.ge.genotype.BitsGenotype;

import java.util.*;

/**
 *
 * @author eric
 */
public class SparseFlipMutation extends AbstractMutation<BitsGenotype> {
  
  @Override
  public List<BitsGenotype> apply(List<BitsGenotype> parents, Random random) {
    BitsGenotype parent = parents.get(0);
    BitsGenotype child = new BitsGenotype(parent.leaves());
    child.set(0, parent);
    int size = Math.max(1, random.nextInt(child.leaves()));
    Collection<Integer> indexes = new HashSet<>();
    while (indexes.size()<size) {
      indexes.add(random.nextInt(child.leaves()));
    }
    for (int index : indexes) {
      child.flip(index);
    }
    return Collections.singletonList(child);
  }  
  
}
