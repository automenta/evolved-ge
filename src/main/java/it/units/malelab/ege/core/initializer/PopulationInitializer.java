/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.initializer;

import it.units.malelab.ege.core.Validator;
import java.util.List;

/**
 *
 * @author eric
 */
public interface PopulationInitializer<G> {
    
  public List<G> build(int n, Validator<G> genotypeValidator);
  
}