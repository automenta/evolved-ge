/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.util;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author eric
 */
public class UtilsTest {
  
  public UtilsTest() {
  }
  
  @Test
  public void testMultisetDiversity() {
    Random random = new Random(1L);
    for (int ss = 2; ss<100; ss++) {
      Collection<Integer> domain = new HashSet<>();
      for (int i = 0; i<ss; i++) {
        domain.add(i);
      }
      //case no diversity
      Multiset<Integer> m = HashMultiset.create();
      m.add(0, 100);
      assertEquals("Multiset with single item should have diversity=0", 0d, Utils.multisetDiversity(m, domain), 0.0001d);
      //case full diversity
      m.clear();
      for (int i = 0; i<ss; i++) {
        m.add(i, 3);
      }
      assertEquals("Multiset with evenly occurring items should have diversity=1", 1d, Utils.multisetDiversity(m, domain), 0.0001d);
      //case random set
      for (int j = 0; j<100; j++) {
        m.clear();
        for (int i = 0; i<ss; i++) {
          m.add(i, random.nextInt(1+j));
        }
        if (m.isEmpty()) {
          m.add(0);
        }
        assertTrue("Multiset diversity should be >= 0", Utils.multisetDiversity(m, domain)>=0);
        assertTrue("Multiset diversity should be <= 1", Utils.multisetDiversity(m, domain)<=1);
      }
    }
  }
  
}
