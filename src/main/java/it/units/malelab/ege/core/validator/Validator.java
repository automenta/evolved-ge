/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.validator;

import java.io.Serializable;
import java.util.Random;

/**
 *
 * @author eric
 */
public interface Validator<G> extends Serializable {
  
    boolean validate(G g, Random random);

}
