/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.ranker;

import com.google.common.collect.Lists;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @author eric
 */
public class ComparableRanker<T> implements Ranker<T> {

    private final Comparator<T> comparator;

    public ComparableRanker(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    @Override
    public List<List<T>> rank(List<T> individuals, Random random) {
        List<T> x = individuals.stream().distinct().collect(toList());
        x.sort(comparator);
        return List.of(x);

//        new HashSet<>();
//        individuals.sort(comparator);
//        ranks.add(new ArrayList<>(List.of(individuals.get(0))));
//        for (int i = 1; i < individuals.size(); i++) {
//            if (comparator.compare(individuals.get(i - 1), individuals.get(i)) < 0) {
//                ranks.add(new ArrayList<>(List.of(individuals.get(i))));
//            } else {
//                ranks.get(ranks.size() - 1).add(individuals.get(i));
//            }
//        }
//        return ranks;
    }

}
