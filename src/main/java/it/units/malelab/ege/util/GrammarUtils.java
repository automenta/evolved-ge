/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.util;

import it.units.malelab.ege.core.Grammar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author eric
 */
public class GrammarUtils {

  public static <T> Map<T, Pair<Double, Double>> computeSymbolsMinMaxDepths(Grammar<T> g) {
    Map<T, Pair<Integer, Boolean>> minDepths = computeSymbolsMinDepths(g);
    Map<T, Triplet<Double, Boolean, Set<T>>> maxDepths = computeSymbolsMaxDepths(g);
    Map<T, Pair<Double, Double>> map = new HashMap<>();
    for (Map.Entry<T, Pair<Integer, Boolean>> tPairEntry: minDepths.entrySet()) {
      map.put(tPairEntry.getKey(), new Pair<>((double) tPairEntry.getValue().first, maxDepths.get(tPairEntry.getKey()).first));
    }
    return map;
  }

  private static <T> Map<T, Pair<Integer, Boolean>> computeSymbolsMinDepths(Grammar<T> g) {
    Map<T, Pair<Integer, Boolean>> map = new HashMap<>();
    map.put(g.getStartingSymbol(), new Pair<>(Integer.MAX_VALUE, false));
      for (List<List<T>> options : ((Map<T, List<List<T>>>) g).values()) {
      for (List<T> option : options) {
        for (T symbol : option) {
            if (!((Map<T, List<List<T>>>) g).containsKey(symbol)) {
            map.put(symbol, new Pair<>(1, true));
          } else {
            map.put(symbol, new Pair<>(Integer.MAX_VALUE, false));
          }
        }
      }
    }
    //compute mins
    while (true) {
      boolean changed = false;
        for (T nonTerminal : ((Map<T, List<List<T>>>) g).keySet()) {
        Pair<Integer, Boolean> pair = map.get(nonTerminal);
          if (pair.second) {
          //this non-terminal is definitely resolved
          continue;
        }
        boolean allResolved = true;
        int minDepth = Integer.MAX_VALUE;
            for (List<T> option : ((Map<T, List<List<T>>>) g).get(nonTerminal)) {
          boolean optionAllResolved = true;
          int optionMaxDepth = 0;
          for (T optionSymbol : option) {
            Pair<Integer, Boolean> optionSymbolPair = map.get(optionSymbol);
            optionAllResolved = optionAllResolved && optionSymbolPair.second;
            optionMaxDepth = Math.max(optionMaxDepth, optionSymbolPair.first);
          }
          allResolved = allResolved && optionAllResolved;
          minDepth = Math.min(minDepth, optionMaxDepth + 1);
        }
        Pair<Integer, Boolean> newPair = new Pair<>(minDepth, allResolved);
        if (!newPair.equals(pair)) {
          map.put(nonTerminal, newPair);
          changed = true;
        }
      }
      if (!changed) {
        break;
      }
    }
    return map;
  }

  private static <T> Map<T, Triplet<Double, Boolean, Set<T>>> computeSymbolsMaxDepths(Grammar<T> g) {
    Map<T, Triplet<Double, Boolean, Set<T>>> map = new HashMap<>();
    map.put(g.getStartingSymbol(), new Triplet<>(0d, false, new HashSet<>()));
      for (List<List<T>> options : ((Map<T, List<List<T>>>) g).values()) {
      for (List<T> option : options) {
        for (T symbol : option) {
            if (!((Map<T, List<List<T>>>) g).containsKey(symbol)) {
            map.put(symbol, new Triplet<>(1d, true, Collections.emptySet()));
          } else {
            map.put(symbol, new Triplet<>(0d, false, new HashSet<>()));
          }
        }
      }
    }
    //compute maxs
    while (true) {
      boolean changed = false;
        for (T nonTerminal : ((Map<T, List<List<T>>>) g).keySet()) {
        Triplet<Double, Boolean, Set<T>> triplet = map.get(nonTerminal);
          Set<T> dependencies = new HashSet<>(triplet.third);
          if (triplet.second) {
          //this non-terminal is definitely resolved
          continue;
        }
        boolean allResolved = true;
        double maxDepth = 0;
            for (List<T> option : ((Map<T, List<List<T>>>) g).get(nonTerminal)) {
          boolean optionAllResolved = true;
          double optionMaxDepth = 0;
          for (T optionSymbol : option) {
            Triplet<Double, Boolean, Set<T>> optionSymbolTriplet = map.get(optionSymbol);
            optionAllResolved = optionAllResolved && optionSymbolTriplet.second;
            optionMaxDepth = Math.max(optionMaxDepth, optionSymbolTriplet.first);
            dependencies.add(optionSymbol);
            dependencies.addAll(optionSymbolTriplet.third);
          }
          allResolved = allResolved && optionAllResolved;
          maxDepth = Math.max(maxDepth, optionMaxDepth + 1);
        }
        if (dependencies.contains(nonTerminal)) {
          allResolved = true;
          maxDepth = Double.POSITIVE_INFINITY;
        }
        Triplet<Double, Boolean, Set<T>> newTriplet = new Triplet<>(maxDepth, allResolved, dependencies);
        if (!newTriplet.equals(triplet)) {
          map.put(nonTerminal, newTriplet);
          changed = true;
        }
      }
      if (!changed) {
        break;
      }
    }
    return map;
  }

}
