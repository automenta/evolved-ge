/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.benchmark.mapper.element;

/**
 *
 * @author eric
 */
public enum Variable implements Element {
  GENOTYPE("g"),
  LIST_N("ln"),
  DEPTH("depth"),
  GL_COUNT_R("g_count_r"),
  GL_COUNT_RW("g_count_rw");
  
  private final String grammarName;

  Variable(String grammarName) {
    this.grammarName = grammarName;
  }

  public String getGrammarName() {
    return grammarName;
  }
  
}
