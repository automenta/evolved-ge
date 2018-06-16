/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Range;
import it.units.malelab.ege.core.Grammar;
import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.Sequence;
import it.units.malelab.ege.core.fitness.MultiObjectiveFitness;
import it.units.malelab.ege.core.listener.EvolverListener;
import it.units.malelab.ege.core.listener.event.EvolutionEvent;
import it.units.malelab.ege.util.distance.Distance;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author eric
 */
public class Utils {

    public static Grammar<String> parseFromFile(File file) throws IOException {
        return parseFromFile(file, "UTF-8");
    }

    private static Grammar<String> parseFromFile(File file, String charset) throws IOException {
        Grammar<String> grammar = new Grammar<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
        String line;
        while ((line = br.readLine()) != null) {
            String[] components = line.split(Pattern.quote(Grammar.RULE_ASSIGNMENT_STRING));
            String toReplaceSymbol = components[0].trim();
            String[] optionStrings = components[1].split(Pattern.quote(Grammar.RULE_OPTION_SEPARATOR_STRING));
            if (grammar.getStartingSymbol() == null) {
                grammar.setStartingSymbol(toReplaceSymbol);
            }
            List<List<String>> options = new ArrayList<>();
            for (String optionString: optionStrings) {
                List<String> symbols = new ArrayList<>();
                for (String symbol: optionString.split("\\s+")) {
                    if (!symbol.trim().isEmpty()) {
                        symbols.add(symbol.trim());
                    }
                }
                if (!symbols.isEmpty()) {
                    options.add(symbols);
                }
            }
            grammar.put(toReplaceSymbol, options);
        }
        br.close();
        return grammar;
    }

    public static double mean(double[] values) {
        if (values.length == 0) {
            return Double.NaN;
        }
        double mean = 0;
        for (double value: values) {
            mean = mean + value;
        }
        return mean / values.length;
    }

    public static <T> Grammar<Pair<T, Integer>> resolveRecursiveGrammar(Grammar<T> grammar, int maxDepth) {
        Grammar<Pair<T, Integer>> resolvedGrammar = new Grammar<>();
        //build tree from recursive
        Node<T> tree = expand(grammar.getStartingSymbol(), grammar, 0, maxDepth);
        //decorate tree with depth
        Node<Pair<T, Integer>> decoratedTree = decorateTreeWithDepth(tree);
        decoratedTree.propagateParentship();
        //rewrite grammar
        resolvedGrammar.put(new Pair<>(grammar.getStartingSymbol(), 0), new ArrayList<>());
        resolvedGrammar.setStartingSymbol(new Pair<>(grammar.getStartingSymbol(), 0));
        while (true) {
            Pair<T, Integer> toFillDecoratedNonTerminal = null;
            for (Pair<T, Integer> decoratedNonTerminal: resolvedGrammar.keySet()) {
                if (resolvedGrammar.get(decoratedNonTerminal).isEmpty()) {
                    toFillDecoratedNonTerminal = decoratedNonTerminal;
                    break;
                }
            }
            if (toFillDecoratedNonTerminal == null) {
                break;
            }
            //look for this non-terminal in the tree
            Iterable<Pair<T, Integer>> decoratedSymbols = contents(findNodeWithContent(decoratedTree, toFillDecoratedNonTerminal));
            Map<T, Pair<T, Integer>> map = new LinkedHashMap<>();
            for (Pair<T, Integer> pair: decoratedSymbols) {
                map.put(pair.first, pair);
            }
            //process original rule
            List<List<T>> options = grammar.get(toFillDecoratedNonTerminal.first);
            for (List<T> option: options) {
                if (map.keySet().containsAll(option)) {
                    List<Pair<T, Integer>> decoratedOption = new ArrayList<>(option.size());
                    for (T symbol: option) {
                        Pair<T, Integer> decoratedSymbol = map.get(symbol);
                        decoratedOption.add(decoratedSymbol);
                        if (!resolvedGrammar.keySet().contains(decoratedSymbol) && grammar.keySet().contains(symbol)) {
                            resolvedGrammar.put(decoratedSymbol, new ArrayList<>());
                        }
                    }
                    resolvedGrammar.get(toFillDecoratedNonTerminal).add(decoratedOption);
                }
            }
            if (resolvedGrammar.get(toFillDecoratedNonTerminal).isEmpty()) {
                throw new IllegalArgumentException(String.format("Cannot expand this grammar with this maxDepth, due to rule for %s.", toFillDecoratedNonTerminal));
            }
        }
        return resolvedGrammar;
    }

    private static <T> Node<T> expand(T symbol, Grammar<T> grammar, int depth, int maxDepth) {
        //TODO something not good here on text.bnf
        if (depth > maxDepth) {
            return null;
        }
        Node<T> node = new Node<>(symbol);
        List<List<T>> options = grammar.get(symbol);
        if (options == null) {
            return node;
        }
        Collection<Node<T>> children = new LinkedHashSet<>();
        for (List<T> option: options) {
            Collection<Node<T>> optionChildren = new LinkedHashSet<>();
            boolean nullNode = false;
            for (T optionSymbol: option) {
                Node<T> child = expand(optionSymbol, grammar, depth + 1, maxDepth);
                if (child == null) {
                    nullNode = true;
                    break;
                }
                optionChildren.add(child);
            }
            if (!nullNode) {
                children.addAll(optionChildren);
            }
        }
        if (children.isEmpty()) {
            return null;
        }
        ((List<Node<T>>) node).addAll(children);
        node.propagateParentship();
        return node;
    }

    private static <T> Node<T> findNodeWithContent(Node<T> tree, T content) {
        if (tree.content.equals(content)) {
            return tree;
        }
        Node<T> foundNode = null;
        for (Node<T> child: tree) {
            foundNode = findNodeWithContent(child, content);
            if (foundNode != null) {
                break;
            }
        }
        return foundNode;
    }

    private static <T> Node<Pair<T, Integer>> decorateTreeWithDepth(Node<T> tree) {
        Node<Pair<T, Integer>> decoratedTree = new Node<>(new Pair<>(tree.content, Iterables.size(tree.getAncestors())));
        for (Node<T> child: tree) {
            decoratedTree.add(decorateTreeWithDepth(child));
        }
        return decoratedTree;
    }

    public static <T> int count(Iterable<T> ts, T matchT) {
        int count = 0;
        for (T t: ts) {
            if (t.equals(matchT)) {
                count = count + 1;
            }
        }
        return count;
    }

    public static <T> Iterable<T> contents(Iterable<Node<T>> nodes) {
//    List<T> contents = new ArrayList<>(nodes.size());
//    for (Node<T> node : nodes) {
//      contents.add(node.content);
//    }
//    return contents;
        return Iterables.transform(nodes, n -> n.content);
    }

    private static <T> void prettyPrintTree(Node<T> node, PrintStream ps) {
        ps.printf("%" + (1 + Iterables.size(node.getAncestors()) * 2) + "s-%s%n", "", node.content);
        for (Node<T> child: node) {
            prettyPrintTree(child, ps);
        }
    }

    public static void broadcast(final EvolutionEvent event, Iterable<? extends EvolverListener> listeners, final ExecutorService executor) {
        for (final EvolverListener listener: listeners) {
            if (listener.getEventClasses().contains(event.getClass())) {
                executor.submit(() -> listener.listen(event));
            }
        }
    }

    public static <T> List<T> getAll(Iterable<Future<List<T>>> futures) throws InterruptedException, ExecutionException {
        List<T> results = new ArrayList<>();
        for (Future<List<T>> future: futures) {
            if (future!=null) {
                List<T> f = future.get();
                if (f != null)
                    results.addAll(f);
            }
        }
        return results;
    }

    public static <T> T selectRandom(Map<T, Double> options, Random random) {
        double sum = 0;
        for (Double rate: options.values()) {
            sum = sum + rate;
        }
        double d = random.nextDouble() * sum;
        for (Map.Entry<T, Double> option: options.entrySet()) {
            if (d < option.getValue()) {
                return option.getKey();
            }
            d = d - option.getValue();
        }
        return (T) options.keySet().toArray()[0];
    }

    public static <K, V> Map<K, V> sameValueMap(V value, K... keys) {
        Map<K, V> map = new LinkedHashMap<>();
        for (K key: keys) {
            map.put(key, value);
        }
        return map;
    }

    public static void printIndividualAncestry(Individual<?, ?, ?> individual, PrintStream ps) {
        printIndividualAncestry(individual, ps, 0);
    }

    private static void printIndividualAncestry(Individual<?, ?, ?> individual, PrintStream ps, int pad) {
        for (int i = 0; i < pad; i++) {
            ps.print(" ");
        }
        ps.printf("'%20.20s' (%3d w/ %10.10s) f=%5.5s%n",
                individual.phenotype.leafNodes(),
                individual.birthDate,
                individual.fitness);
        for (Individual<?, ?, ?> parent: individual.parents) {
            printIndividualAncestry(parent, ps, pad + 2);
        }
    }

    public static List<Range<Integer>> slices(Range<Integer> range, int pieces) {
        List<Integer> sizes = new ArrayList<>(pieces);
        for (int i = 0; i < pieces; i++) {
            sizes.add(1);
        }
        return slices(range, sizes);
    }

    public static List<Range<Integer>> slices(Range<Integer> range, List<Integer> sizes) {
        int length = range.upperEndpoint() - range.lowerEndpoint();
        int sumOfSizes = 0;
        for (int size: sizes) {
            sumOfSizes = sumOfSizes + size;
        }
        if (sumOfSizes > length) {
            Iterable<Integer> originalSizes = new ArrayList<>(sizes);
            sizes = new ArrayList<>(sizes.size());
            int oldSumOfSizes = sumOfSizes;
            sumOfSizes = 0;
            for (int originalSize: originalSizes) {
                int newSize = (int) Math.round((double) originalSize / oldSumOfSizes);
                sizes.add(newSize);
                sumOfSizes = sumOfSizes + newSize;
            }
        }
        int minSize = (int) Math.floor((double) length / sumOfSizes);
        int missing = length - minSize * sumOfSizes;
        int[] rangeSize = new int[sizes.size()];
        for (int i = 0; i < rangeSize.length; i++) {
            rangeSize[i] = minSize * sizes.get(i);
        }
        int c = 0;
        while (missing > 0) {
            rangeSize[c % rangeSize.length] = rangeSize[c % rangeSize.length] + 1;
            c = c + 1;
            missing = missing - 1;
        }
        List<Range<Integer>> ranges = new ArrayList<>(sizes.size());
        int offset = range.lowerEndpoint();
        for (int aRangeSize: rangeSize) {
            ranges.add(Range.closedOpen(offset, offset + aRangeSize));
            offset = offset + aRangeSize;
        }
        return ranges;
    }

    public static <T> boolean validate(Node<T> tree, Grammar<T> grammar) {
        if (tree == null) {
            return false;
        }
        if (!tree.content.equals(grammar.getStartingSymbol())) {
            return false;
        }
        Set<T> terminals = new LinkedHashSet<>();
        for (List<List<T>> options: grammar.values()) {
            for (List<T> option: options) {
                terminals.addAll(option);
            }
        }
        terminals.removeAll(grammar.keySet());
        return innerValidate(tree, grammar, terminals);
    }

    private static <T> boolean innerValidate(Node<T> tree, Grammar<T> grammar, Set<T> terminals) {
        //validate node content
        if (!grammar.keySet().contains(tree.content) && !terminals.contains(tree.content)) {
            return false;
        }
        if (terminals.contains(tree.content)) {
            return true;
        }
        //validate node children sequence (option)
        List<T> childContents = new ArrayList<>();
        for (Node<T> child: tree) {
            childContents.add(child.content);
        }
        if (!grammar.get(tree.content).contains(childContents)) {
            return false;
        }
        for (Node<T> child: tree) {
            if (!innerValidate(child, grammar, terminals)) {
                return false;
            }
        }
        return true;
    }

    public static double pearsonCorrelation(List<Pair<Double, Double>> values) {
        if (values.isEmpty() || values.size() == 1) {
            return Double.NaN;
        }
        double[] x = new double[values.size()];
        double[] y = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            x[i] = values.get(i).first;
            y[i] = values.get(i).second;
        }
        return new PearsonsCorrelation().correlation(x, y);
    }

    public static <T> Sequence<T> from(final Iterable<T> list) {
        return from(Lists.newArrayList(list));
    }

    public static <T> Sequence<T> from(final List<T> list) {
        return new Sequence<>() {
            @Override
            public T content(int index) {
                return list.get(index);
            }

            @Override
            public int leaves() {
                return list.size();
            }

            @Override
            @Deprecated
            public Sequence<T> clone() {
                return from(new ArrayList<T>(list));
                //return this;
            }

            @Override
            public void replace(int index, T t) {
                throw new UnsupportedOperationException("Cannot set in read-only view of a list");
            }
        };
    }

    public static <T> Node<T> node(T t, Node<T>... children) {
        Node<T> n = new Node<>(t);
        Collections.addAll(n, children);
        return n;
    }

    public static String formatName(String name, String format, boolean doFormat) {
        if (!doFormat) {
            return name;
        }
        String acronym;
        String[] pieces = name.split("\\.");
        StringBuilder acronymBuilder = new StringBuilder();
        for (String piece: pieces) {
            acronymBuilder.append(piece.substring(0, 1));
        }
        acronym = acronymBuilder.toString();
        acronym = pad(acronym, formatSize(format), doFormat);
        return acronym;
    }

    public static int formatSize(String format) {
        int size = 0;
        Matcher matcher = Pattern.compile("\\d++").matcher(format);
        if (matcher.find()) {
            size = Integer.parseInt(matcher.group());
            if (format.contains("+")) {
                size = size + 1;
            }
            return size;
        }
        return String.format(format, (Object[]) null).length();
    }

    public static String pad(String s, int length, boolean doFormat) {
        StringBuilder sBuilder = new StringBuilder(s);
        while (doFormat && sBuilder.length() < length) {
            sBuilder.insert(0, ' ');
        }
        s = sBuilder.toString();
        return s;
    }

    public static <T> Set<T> maximallySparseSubset(Map<T, MultiObjectiveFitness<Double>> points, int n) {
        Collection<T> remainingPoints = new LinkedHashSet<>(points.keySet());
        Set<T> selectedPoints = new LinkedHashSet<>();
        Distance<MultiObjectiveFitness<Double>> d = (Distance<MultiObjectiveFitness<Double>>) (mof1, mof2) -> {
            double d1 = 0;
            for (int i = 0; i < Math.min(mof1.getValue().length, mof2.getValue().length); i++) {
                d1 = d1 + Math.pow(mof1.getValue()[i] - mof2.getValue()[i], 2d);
            }
            return Math.sqrt(d1);
        };
        while (!remainingPoints.isEmpty() && selectedPoints.size() < n) {
            T selected = remainingPoints.iterator().next();
            if (!selectedPoints.isEmpty()) {
                //add point with max distance from closest point
                double maxMinD = Double.NEGATIVE_INFINITY;
                for (T t: remainingPoints) {
                    double minD = Double.POSITIVE_INFINITY;
                    for (T otherT: selectedPoints) {
                        minD = Math.min(minD, d.d(points.get(t), points.get(otherT)));
                    }
                    if (minD > maxMinD) {
                        maxMinD = minD;
                        selected = t;
                    }
                }
                //sort
            }
            remainingPoints.remove(selected);
            selectedPoints.add(selected);
        }
        return selectedPoints;
    }

    public static double avgDepth(Node tree) {
        List<Double> depths = new ArrayList<>();
        collectLeafDepths(tree, 0, depths);
        double[] values = new double[depths.size()];
        for (int i = 0; i < depths.size(); i++) {
            values[i] = depths.get(i);
        }
        return StatUtils.mean(values);
    }

    private static void collectLeafDepths(Node<?> tree, int d, List<Double> depths) {
        if (tree.isEmpty()) {
            depths.add((double) d);
            return;
        }
        for (Node<?> child: tree) {
            collectLeafDepths(child, d + 1, depths);
        }
    }

    public static double multisetDiversity(Multiset m, Collection d) {
        double[] counts = new double[d.size()];
        int i = 0;
        for (Object possibleValue: d) {
            counts[i] = m.count(possibleValue);
            i = i + 1;
        }
        return 1d - normalizedVariance(counts);
    }

    public static double normalizedVariance(double[] values) {
        double sumOfSquares = 0;
        double sum = 0;
        for (double count: values) {
            sumOfSquares = sumOfSquares + count * count;
            sum = sum + count;
        }
        double minOfSumOfSquares = sum * sum / values.length;
        double maxOfSumOfSquares = sum * sum;
        return (sumOfSquares - minOfSumOfSquares) / (maxOfSumOfSquares - minOfSumOfSquares);
    }

    public static <V> Future<V> future(final V v) {
        return new Future<>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public V get() {
                return v;
            }

            @Override
            public V get(long timeout, TimeUnit unit) {
                return v;
            }
        };
    }

    public static class MapBuilder<K, V> {

        private final LinkedHashMap<K, V> map;

        public MapBuilder() {
            map = new LinkedHashMap<>();
        }

        public MapBuilder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        public Map<K, V> build() {
            return map;
        }

    }

}
