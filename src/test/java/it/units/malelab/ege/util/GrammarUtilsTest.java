/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.util;

import it.units.malelab.ege.core.Grammar;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author eric
 */
public class GrammarUtilsTest {
  
  public GrammarUtilsTest() {
  }

  @Test
  public void testComputeSymbolsMinMaxDepths() throws IOException {
    String[] grammars = new String[]{"grammars/max-grammar.bnf", "grammars/text.bnf", "grammars/mapper.bnf", "grammars/symbolic-regression.bnf"};
    for (String grammar : grammars) {
      Grammar<String> g = Utils.parseFromFile(new File(grammar));
      Map<String, Pair<Double, Double>> depths = GrammarUtils.computeSymbolsMinMaxDepths(g);
      assertTrue("Should contain all non-terminal symbols.", depths.keySet().containsAll(g.keySet()));
      for (Map.Entry<String, Pair<Double, Double>> stringPairEntry: depths.entrySet()) {
        if (!g.containsKey(stringPairEntry.getKey())) {
          assertEquals("Terminal should have min=1", 1, stringPairEntry.getValue().first, 0.01f);
          assertEquals("Terminal should have max=1", 1, stringPairEntry.getValue().second, 0.01f);
        } else {
          assertTrue("Should have min<=max", stringPairEntry.getValue().first <= stringPairEntry.getValue().second);
        }
      }
    }
  }
  
}
