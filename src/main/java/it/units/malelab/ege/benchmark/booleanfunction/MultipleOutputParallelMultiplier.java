/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.benchmark.booleanfunction;

import it.units.malelab.ege.core.Grammar;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.PhenotypePrinter;
import it.units.malelab.ege.core.Problem;
import it.units.malelab.ege.core.fitness.NumericFitness;
import it.units.malelab.ege.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author eric
 */
public class MultipleOutputParallelMultiplier extends Problem<String, NumericFitness> {

    public MultipleOutputParallelMultiplier(final int size) throws IOException {
        super(
                buildGrammar(size),
                new MOPMErrors(size),
                null,
                (PhenotypePrinter<String>) node -> {
                    if (Node.EMPTY_TREE.equals(node)) {
                        return null;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(':');
                        }
                        sb.append(BooleanUtils.transform(node.get(i)));
                    }
                    return sb.toString();
                }
        );
    }

    private static Grammar<String> buildGrammar(int size) throws IOException {
        Grammar<String> grammar = Utils.parseFromFile(new File("grammars/boolean-parity-var.bnf"));
        List<List<String>> vars = new ArrayList<>(2*size);
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < size; i++) {
                vars.add(List.of("b" + j + '.' + i));
            }
        }
        grammar.put("<v>", vars);
        List<String> output = new ArrayList<>(2*size);
        for (int i = 0; i < 2 * size; i++) {
            output.add("<e>");
        }
        grammar.put("<o>", List.of(output));
        grammar.setStartingSymbol("<o>");
        return grammar;
    }

}
