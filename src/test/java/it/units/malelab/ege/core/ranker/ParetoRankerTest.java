/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.ranker;

import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.fitness.MultiObjectiveFitness;

import java.util.*;

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
public class ParetoRankerTest {
  
  public ParetoRankerTest() {
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
   * Test of rank method, of class ParetoRanker.
   */
  @Test
  public void testRank() {
    Ranker instance = new ParetoRanker();
    Individual<String, String, MultiObjectiveFitness> i0 = new Individual<>("0", new Node<>("0"), new MultiObjectiveFitness(2, 1), 0, null, null);
    Individual<String, String, MultiObjectiveFitness> i1 = new Individual<>("1", new Node<>("1"), new MultiObjectiveFitness(1, 2), 0, null, null);
    Individual<String, String, MultiObjectiveFitness> i2 = new Individual<>("2", new Node<>("2"), new MultiObjectiveFitness(1, 3), 0, null, null);
    Individual<String, String, MultiObjectiveFitness> i3 = new Individual<>("3", new Node<>("3"), new MultiObjectiveFitness(3, 1), 0, null, null);
    Individual<String, String, MultiObjectiveFitness> i4 = new Individual<>("4", new Node<>("4"), new MultiObjectiveFitness(4, 4), 0, null, null);
    Individual<String, String, MultiObjectiveFitness> i5 = new Individual<>("5", new Node<>("5"), new MultiObjectiveFitness(5, 5), 0, null, null);
    List<Individual<String, String, MultiObjectiveFitness>> pop = Arrays.asList(i0, i1, i2, i3, i4, i5);
    for (int i = 0; i<5; i++) {
      List<List<Individual<String, String, MultiObjectiveFitness>>> ranked = instance.rank(pop, null);
      assertTrue("i0 rank should be in list 0", ranked.get(0).contains(i0));
      assertTrue("i1 rank should be in list 0", ranked.get(0).contains(i1));
      assertTrue("i2 rank should be in list 1", ranked.get(1).contains(i2));
      assertTrue("i3 rank should be in list 1", ranked.get(1).contains(i3));
      assertTrue("i4 rank should be in list 2", ranked.get(2).contains(i4));
      assertTrue("i5 rank should be in list 3", ranked.get(3).contains(i5));
      Collections.shuffle(pop, new Random(1L));
    }
  }

  /**
   * Test of compare method, of class ParetoRanker.
   */
  @Test
  public void testCompare() {
    MultiObjectiveFitness f0 = new MultiObjectiveFitness(2, 1);
    MultiObjectiveFitness f1 = new MultiObjectiveFitness(1, 2);
    MultiObjectiveFitness f2 = new MultiObjectiveFitness(2, 2);
    Comparator instance = new ParetoRanker();
    assertEquals("f0 vs f1 should be 0", 0, instance.compare(f0, f1));
    assertEquals("f1 vs f0 should be 0", 0, instance.compare(f1, f0));
    assertEquals("f1 vs f2 should be -1", -1, instance.compare(f1, f2));
    assertEquals("f2 vs f0 should be 1", 1, instance.compare(f2, f0));
  }
  
}
