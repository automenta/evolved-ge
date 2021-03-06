/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.benchmark;

import it.units.malelab.ege.core.fitness.BinaryClassification;
import it.units.malelab.ege.util.Utils;

import java.util.*;

/**
 *
 * @author eric
 */
class RegexMatch extends BinaryClassification<String, String> {

  private final String alphabet;
  private final int maxLength;
  private final int size;
  private final String[] regexes;

  public RegexMatch(String alphabet, int maxLength, int size, Random random, String... regexes) {
    super(
            new ArrayList<>(),
            new ArrayList<>(),
            (instance, classifier) -> {
              StringBuilder sb = new StringBuilder();
              for (String leaf: Utils.contents(classifier.leafNodes())) {
                sb.append(leaf);
              }
              return instance.matches(sb.toString());
            }
    );
    this.alphabet = alphabet;
    this.maxLength = maxLength;
    this.size = size;
    this.regexes = regexes;
    //generate strings    
    Collection<String> strings = new LinkedHashSet<>();
    while (strings.size() < size) {
      int length = random.nextInt(maxLength - 1) + 1;
      StringBuilder sb = new StringBuilder();
      while (sb.length() < length) {
        sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
      }
      strings.add(sb.toString());
    }
    //classify strings
    for (String string : strings) {
      boolean matched = false;
      for (String regex : regexes) {
        if (string.matches(regex)) {
          matched = true;
          break;
        }
      }
      if (matched) {
        getPositives().add(string);
      } else {
        getNegatives().add(string);
      }
    }
  }

  public String getAlphabet() {
    return alphabet;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public int getSize() {
    return size;
  }

  public String[] getRegexes() {
    return regexes;
  }

}
