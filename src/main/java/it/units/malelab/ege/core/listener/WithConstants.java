/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.listener;

import java.util.Map;

/**
 *
 * @author eric
 */
interface WithConstants {
  
  void updateConstants(Map<String, Object> newConstants);
  
}
