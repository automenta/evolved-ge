/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.cfggp.initializer;

import it.units.malelab.ege.core.Grammar;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author eric
 */
public class FullTreeFactory<T> extends GrowTreeFactory<T> {

  public FullTreeFactory(int maxDepth, Grammar<T> grammar) {
    super(maxDepth, grammar);
  }
  
  @Override
  public Node<T> build(Random random) {
    Node<T> tree = null;
    while (tree == null) {
      tree = build(random, grammar.getStartingSymbol(), super.maxDepth);
    }
    return tree;
  }  

  public Node<T> build(Random random, T symbol, int targetDepth) {
    if (targetDepth<0) {
      return null;
    }
    Node<T> tree = new Node<>(symbol);
      if (((Map<T, List<List<T>>>) grammar).containsKey(symbol)) {
      //a non-terminal
          List<List<T>> options = ((Map<T, List<List<T>>>) grammar).get(symbol);
      List<List<T>> availableOptions = new ArrayList<>();
      //general idea: try the following
      //1. choose expansion with min,max including target depth
      //2. choose expansion
      for (List<T> option : options) {        
        Pair<Double, Double> minMax = optionMinMaxDepth(option);
        if (((targetDepth-1)>= minMax.first)&&((targetDepth-1)<= minMax.second)) {
          availableOptions.add(option);
        }
      }
      if (availableOptions.isEmpty()) {
        availableOptions.addAll(options);
      }
      int optionIndex = random.nextInt(availableOptions.size());
      for (int i = 0; i<availableOptions.get(optionIndex).size(); i++) {
        Node<T> child = build(random, availableOptions.get(optionIndex).get(i), targetDepth-1);
        if (child == null) {
          return null;
        }
          tree.add(child);
      }
    }
    return tree;
  }
  
}
