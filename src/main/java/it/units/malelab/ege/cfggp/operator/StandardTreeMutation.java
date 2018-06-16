/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.cfggp.operator;

import it.units.malelab.ege.cfggp.initializer.GrowTreeFactory;
import it.units.malelab.ege.core.Grammar;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.operator.AbstractMutation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 * @author eric
 */
public class StandardTreeMutation<T>  extends AbstractMutation<Node<T>> {
  
  private final int maxDepth;
  private final GrowTreeFactory<T> factory;

  public StandardTreeMutation(int maxDepth, Grammar<T> grammar) {
    this.maxDepth = maxDepth;
    factory = new GrowTreeFactory<>(0, grammar);
  }

  @Override
  public List<Node<T>> apply(List<Node<T>> parents, Random random) {
    Node<T> child = new Node<>(parents.get(0));
    List<Node<T>> nonTerminalNodes = new ArrayList<>();
    getNonTerminalNodes(child, nonTerminalNodes);
    Collections.shuffle(nonTerminalNodes, random);
    boolean done = false;
    for (Node<T> toReplaceSubTree : nonTerminalNodes) {
      Node<T> newSubTree = factory.build(random, toReplaceSubTree.content, toReplaceSubTree.depth());
      if (newSubTree!=null) {
        toReplaceSubTree.clear();
        toReplaceSubTree.addAll(newSubTree);
        done = true;
        break;
      }
    }
    if (!done) {
      return null;
    }
    return Collections.singletonList(child);
  }
  
  private void getNonTerminalNodes(Node<T> node, List<Node<T>> nodes) {
    if (!node.isEmpty()) {
      nodes.add(node);
      for (int i = 0, nodeSize = node.size(); i < nodeSize; i++) {
        Node<T> child = node.get(i);
        getNonTerminalNodes(child, nodes);
      }
    }
  }
  
  
}
