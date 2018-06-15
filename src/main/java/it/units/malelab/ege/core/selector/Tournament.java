/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author eric
 */
public class Tournament<T> implements Selector<T> {
  
  private final int size;

  public Tournament(int size) {
    this.size = size;
  }

  @Override
  public T select(List<List<T>> ts, Random random) {
    SortedMap<Integer, List<T>> selected = new TreeMap<>();
    for (int i = 0; i<size; i++) {
      int rankIndex = random.nextInt(ts.size());
      int index = random.nextInt(ts.get(rankIndex).size());
        List<T> localTs = selected.computeIfAbsent(rankIndex, k -> new ArrayList<>());
        localTs.add(ts.get(rankIndex).get(index));
    }
    return selected.get(selected.firstKey()).get(0);
  }

  @Override
  public String toString() {
    return "Tournament{" + "size=" + size + '}';
  }

}
