/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.cfggp.operator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.operator.AbstractCrossover;

import java.util.*;

/**
 * @author eric
 */
public class StandardTreeCrossover<T> extends AbstractCrossover<Node<T>> {

    private final int maxDepth;

    public StandardTreeCrossover(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public List<Node<T>> apply(List<Node<T>> parents, Random random) {
        //build maps of leaf-subtrees
        Node<T> child1 = new Node<>(parents.get(0));
        Node<T> child2 = new Node<>(parents.get(1));
        child1.propagateParentship();
        child2.propagateParentship();
        Multimap<T, Node<T>> child1subtrees = ArrayListMultimap.create();
        Multimap<T, Node<T>> child2subtrees = ArrayListMultimap.create();
        populateMultimap(child1, child1subtrees);
        populateMultimap(child2, child2subtrees);
        //build common non-terminals
        List<T> nonTerminals = new ArrayList<>(child1subtrees.keySet());
        nonTerminals.retainAll(child2subtrees.keySet());
        if (nonTerminals.isEmpty()) {
            return null;
        }
        Collections.shuffle(nonTerminals, random);
        //iterate (just once, if successfully) on non-terminals
        boolean done = false;
        for (T chosenNonTerminal: nonTerminals) {
            List<Node<T>> subtrees1 = new ArrayList<>(child1subtrees.get(chosenNonTerminal));
            List<Node<T>> subtrees2 = new ArrayList<>(child2subtrees.get(chosenNonTerminal));
            Collections.shuffle(subtrees1, random);
            Collections.shuffle(subtrees2, random);
            for (Node<T> subtree1: subtrees1) {
                int subtree1AncestorsSize = Iterables.size(subtree1.getAncestors());
                int subtree1Depth = subtree1.depth();
                for (Node<T> subtree2: subtrees2) {
                    if (!subtree1.equals(subtree2)) {
                        if ((subtree1AncestorsSize + subtree2.depth() <= maxDepth) && (Iterables.size(subtree2.getAncestors()) + subtree1Depth <= maxDepth)) {
                            subtree1.clearParents();
                            subtree2.clearParents();
                            Collection<Node<T>> swappingChildren = new ArrayList<>(subtree1);
                            subtree1.clear();
                            subtree1.addAll(subtree2);
                            subtree2.clear();

                            subtree2.addAll(swappingChildren);
                            done = true;
                            break;
                        }
                    }
                }
                if (done) {
                    break;
                }
            }
            if (done) {
                break;
            }
        }
        if (!done) {
            return null;
        }
        //return
        return List.of(child1, child2);
    }

    private void populateMultimap(Node<T> node, Multimap<T, Node<T>> multimap) {
        if (node.isEmpty()) {
            return;
        }
        multimap.put(node.content, node);
        for (Node<T> child: node) {
            populateMultimap(child, multimap);
        }

    }

}
