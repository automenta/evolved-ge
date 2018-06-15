/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.ge.operator;

import it.units.malelab.ege.core.operator.AbstractMutation;
import it.units.malelab.ege.ge.genotype.BitsGenotype;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 * @author eric
 */
public class LengthChanger extends AbstractMutation<BitsGenotype> {
  
  private final double maxRatio;

  public LengthChanger(double maxRatio) {
    this.maxRatio = maxRatio;
  }

  @Override
  public List<BitsGenotype> apply(List<BitsGenotype> parents, Random random) {
    BitsGenotype parent = parents.get(0);
    int newBits = (int)Math.round(parent.leaves() *random.nextDouble()*maxRatio);
    if (newBits==0) {
      return Collections.singletonList(new BitsGenotype(parent.leaves(), parent.asBitSet()));
    }
    boolean shorter = random.nextBoolean();
    BitsGenotype child;
    if (shorter) {
      int index = random.nextInt(parent.leaves()-newBits)+newBits;
      child = new BitsGenotype(parent.leaves()-newBits);
      if ((index-newBits)>0) {
        child.set(0, parent.slice(0, index-newBits));
      }
      child.set(index-newBits, parent.slice(index, parent.leaves()));
    } else {
      int index = random.nextInt(parent.leaves());
      child = new BitsGenotype(parent.leaves()+newBits);
      if (index>0) {
        child.set(0, parent.slice(0, index));
      }
      child.set(index+newBits, parent.slice(index, parent.leaves()));
    }
    return Collections.singletonList(child);
  }  
  
}
