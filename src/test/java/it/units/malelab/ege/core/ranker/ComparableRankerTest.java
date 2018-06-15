/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.ranker;

import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.fitness.NumericFitness;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author eric
 */
public class ComparableRankerTest {

  public ComparableRankerTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of rank method, of class ComparableFitnessRanker.
   */
  @Test
  public void testRank() {
    Ranker<Individual<?, ?, NumericFitness>> instance = new ComparableRanker<>(Comparator.comparing((o) -> o.fitness));
    Individual<?, ?, NumericFitness> i0 = new Individual<>(null, null, new NumericFitness(0d), 0, null, null);
    Individual<?, ?, NumericFitness> i1 = new Individual<>(null, null, new NumericFitness(1d), 0, null, null);
    Individual<?, ?, NumericFitness> i2 = new Individual<>(null, null, new NumericFitness(2d), 0, null, null);
    Individual<?, ?, NumericFitness> i3 = new Individual<>(null, null, new NumericFitness(2d), 0, null, null);
    List<Individual<?, ?, NumericFitness>> pop = Arrays.asList(i0, i1, i2, i3);
    for (int i = 0; i < 5; i++) {
      List<List<Individual<?, ?, NumericFitness>>> ranked = instance.rank(pop, null);
      assertTrue("i0 rank should be in list 0", ranked.get(0).contains(i0));
      assertTrue("i1 rank should be in list 1", ranked.get(1).contains(i1));
      assertTrue("i2 rank should be in list 2", ranked.get(2).contains(i2));
      assertTrue("i3 rank should be in list 2", ranked.get(2).contains(i3));
      Collections.shuffle(pop, new Random(1L));
    }
  }

}
