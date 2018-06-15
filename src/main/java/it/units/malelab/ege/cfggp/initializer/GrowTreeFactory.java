/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.cfggp.initializer;

import it.units.malelab.ege.core.Factory;
import it.units.malelab.ege.core.Grammar;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.util.GrammarUtils;
import it.units.malelab.ege.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author eric
 */
public class GrowTreeFactory<T> implements Factory<Node<T>> {

    final int maxDepth;
    final Grammar<T> grammar;

    private final Map<T, Pair<Double, Double>> nonTerminalDepths;

    public GrowTreeFactory(int maxDepth, Grammar<T> grammar) {
        this.maxDepth = maxDepth;
        this.grammar = grammar;
        nonTerminalDepths = GrammarUtils.computeSymbolsMinMaxDepths(grammar);
    }

    @Override
    public Node<T> build(Random random) {
        return build(random, grammar.getStartingSymbol(), maxDepth);
    }

    <T> Pair<Double, Double> optionMinMaxDepth(Iterable<T> option) {
        double min = 0d;
        double max = 0d;
        for (T symbol: option) {
            Pair<Double, Double> p = nonTerminalDepths.get(symbol);
            min = Math.max(min, p.first);
            max = Math.max(max, p.second);
        }
        return new Pair<>(min, max);
    }

    public Node<T> build(Random random, T symbol, int targetDepth) {
        if (targetDepth < 0) {
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
            for (List<T> option: options) {
                Pair<Double, Double> minMax = optionMinMaxDepth(option);
                if (((targetDepth - 1) >= minMax.first) && ((targetDepth - 1) <= minMax.second)) {
                    availableOptions.add(option);
                }
            }
            if (availableOptions.isEmpty()) {
                availableOptions.addAll(options);
            }
            int optionIndex = random.nextInt(availableOptions.size());
            //choose one index to force as full
            List<Integer> availableFullIndexes = new ArrayList<>();
            for (int i = 0; i < availableOptions.get(optionIndex).size(); i++) {
                Pair<Double, Double> minMax = nonTerminalDepths.get(availableOptions.get(optionIndex).get(i));
                if (((targetDepth - 1) >= minMax.first) && ((targetDepth - 1) <= minMax.second)) {
                    availableFullIndexes.add(i);
                }
            }
            int fullIndex = random.nextInt(availableOptions.get(optionIndex).size());
            if (!availableFullIndexes.isEmpty()) {
                fullIndex = availableFullIndexes.get(random.nextInt(availableFullIndexes.size()));
            }
            for (int i = 0; i < availableOptions.get(optionIndex).size(); i++) {
                int childTargetDepth = targetDepth - 1;
                Pair<Double, Double> minMax = nonTerminalDepths.get(availableOptions.get(optionIndex).get(i));
                if ((i != fullIndex) && (childTargetDepth > minMax.first)) {
                    childTargetDepth = random.nextInt(childTargetDepth - minMax.first.intValue()) + minMax.first.intValue();
                }
                Node<T> child = build(random, availableOptions.get(optionIndex).get(i), childTargetDepth);
                if (child == null) {
                    return null;
                }
                tree.children.add(child);
            }
        }
        return tree;
    }

}
