/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.ge.mapper;

import it.units.malelab.ege.ge.genotype.Genotype;
import it.units.malelab.ege.core.grammar.Grammar;

/**
 *
 * @author eric
 */
public abstract class AbstractMapper<G extends Genotype, T> implements Mapper<G, T> {

  protected final Grammar<T> grammar;

  public AbstractMapper(Grammar<T> grammar) {
    this.grammar = grammar;
  }

  public Grammar<T> getGrammar() {
    return grammar;
  }
  
}