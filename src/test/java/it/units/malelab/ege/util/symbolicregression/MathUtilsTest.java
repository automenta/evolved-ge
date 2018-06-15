/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.util.symbolicregression;

import it.units.malelab.ege.benchmark.symbolicregression.MathUtils;
import it.units.malelab.ege.core.Node;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author eric
 */
public class MathUtilsTest {

  public MathUtilsTest() {
  }

  @Test
  public void testCombinedValuesMap_Map() {
    Map<String, double[]> flatMap = new LinkedHashMap<>();
    flatMap.put("a", new double[]{0, 1});
    flatMap.put("b", new double[]{0, 1, 2});
    Map<String, double[]> map = MathUtils.combinedValuesMap(flatMap);
    assertEquals("keys should be the same as input", flatMap.keySet(), map.keySet());
    assertArrayEquals("'a' values should be [0,1,0,1,0,1]", new double[]{0, 1, 0, 1, 0, 1}, map.get("a"), 0);
    assertArrayEquals("'b' values should be [0,0,1,1,2,2]", new double[]{0, 0, 1, 1, 2, 2}, map.get("b"), 0);
  }

  @Test
  public void testCompute() {
    Map<String, double[]> flatMap = new LinkedHashMap<>();
    flatMap.put("a", new double[]{0, 1, 2});
    flatMap.put("b", new double[]{1, 2, 3});
    Node<String> f = new Node<>("<expr>");
    f.children.add(new Node<>("<op>"));
    f.children.add(new Node<>("<expr>"));
    f.children.add(new Node<>("<expr>"));
    f.children.get(0).children.add(new Node<>("+"));
    f.children.get(1).children.add(new Node<>("<var>"));
    f.children.get(2).children.add(new Node<>("<var>"));
    f.children.get(1).children.get(0).children.add(new Node<>("a"));
      f.children.get(2).children.get(0).children.add(new Node<>("b"));
    double[] result = MathUtils.compute(MathUtils.transform(f), flatMap, 3);
    assertArrayEquals("a+b should give [1,3,5]", new double[]{1, 3, 5}, result, 0);
  }

}
