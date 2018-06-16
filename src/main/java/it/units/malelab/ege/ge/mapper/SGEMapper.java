/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.ge.mapper;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import it.units.malelab.ege.core.Grammar;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.mapper.AbstractMapper;
import it.units.malelab.ege.ge.genotype.SGEGenotype;
import it.units.malelab.ege.util.Pair;
import it.units.malelab.ege.util.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static it.units.malelab.ege.ge.mapper.StandardGEMapper.BIT_USAGES_INDEX_NAME;

/**
 * @author eric
 */
public class SGEMapper<T> extends AbstractMapper<SGEGenotype<T>, T> {

    private final Grammar<Pair<T, Integer>> nonRecursiveGrammar;
    private final Map<Pair<T, Integer>, List<Integer>> geneBounds;
    private final int maxDepth;
    private final Map<Pair<T, Integer>, Integer> geneFirstIndexes;

    public SGEMapper(int maxDepth, Grammar<T> grammar) {
        super(grammar);
        this.maxDepth = maxDepth;
        nonRecursiveGrammar = Utils.resolveRecursiveGrammar(grammar, maxDepth);
        geneBounds = new LinkedHashMap<>();
        geneFirstIndexes = new LinkedHashMap<>();
        final int[] counter = {0};
        nonRecursiveGrammar.forEach((key, value) -> {

            int n = maximumExpansions(key, nonRecursiveGrammar);
            List<Integer> bounds = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                bounds.add(value.size());
            }
            geneBounds.put(key, bounds);
            geneFirstIndexes.put(key, counter[0]);
            counter[0] = counter[0] + bounds.size();
        });
    }

    private static <E> int maximumExpansions(E nonTerminal, Grammar<E> g) {
        //assume non recursive grammar
        if (nonTerminal.equals(g.getStartingSymbol())) {
            return 1;
        }
        final int[] count = {0};
        g.forEach((key, value) -> {
            int maxCount = Integer.MIN_VALUE;
            for (List<E> option: value) {
                int optionCount = 0;
                for (E optionSymbol: option) {
                    if (optionSymbol.equals(nonTerminal)) {
                        optionCount = optionCount + 1;
                    }
                }
                maxCount = Math.max(maxCount, optionCount);
            }
            if (maxCount > 0) {
                count[0] = count[0] + maxCount * maximumExpansions(key, g);
            }
        });
        return count[0];
    }

    public Node<T> map(SGEGenotype<T> genotype, Map<String, Object> report) {
        int[] usages = new int[genotype.leaves()];
        //map
        Multiset<Pair<T, Integer>> expandedSymbols = LinkedHashMultiset.create();
        Node<Pair<T, Integer>> tree = new Node<>(nonRecursiveGrammar.getStartingSymbol());
        while (true) {
            Node<Pair<T, Integer>> nodeToBeReplaced = null;
            for (Node<Pair<T, Integer>> node: tree.leafNodes()) {
                if (nonRecursiveGrammar.keySet().contains(node.content)) {
                    nodeToBeReplaced = node;
                    break;
                }
            }
            if (nodeToBeReplaced == null) {
                break;
            }
            //get codon
            List<Integer> values = genotype.getGenes().get(nodeToBeReplaced.content);
            int value = values.get(expandedSymbols.count(nodeToBeReplaced.content));
            int usageIndex = geneFirstIndexes.get(nodeToBeReplaced.content) + expandedSymbols.count(nodeToBeReplaced.content);
            usages[usageIndex] = usages[usageIndex] + 1;
            List<List<Pair<T, Integer>>> options = nonRecursiveGrammar.get(nodeToBeReplaced.content);
            //add children
            for (Pair<T, Integer> symbol: options.get(value)) {
                Node<Pair<T, Integer>> newChild = new Node<>(symbol);
                nodeToBeReplaced.add(newChild);
            }
            expandedSymbols.add(nodeToBeReplaced.content);
        }
        report.put(BIT_USAGES_INDEX_NAME, usages);
        return transform(tree);
    }

    private Node<T> transform(Node<Pair<T, Integer>> pairNode) {
        Node<T> node = new Node<>(pairNode.content.first);
        for (Node<Pair<T, Integer>> pairChild: pairNode) {
            ((List<Node<T>>) node).add(transform(pairChild));
        }
        return node;
    }

    public Map<Pair<T, Integer>, List<Integer>> getGeneBounds() {
        return geneBounds;
    }

    @Override
    public String toString() {
        return "SGEMapper{" + "maxDepth=" + maxDepth + '}';
    }

}
