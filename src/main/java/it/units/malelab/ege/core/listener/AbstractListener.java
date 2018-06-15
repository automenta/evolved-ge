/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.listener;

import it.units.malelab.ege.core.fitness.Fitness;
import it.units.malelab.ege.core.listener.event.EvolutionEvent;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author eric
 */
public abstract class AbstractListener<G, T, F extends Fitness> implements EvolverListener<G, T, F> {
  
  private final Set<Class<? extends EvolutionEvent>> eventClasses;

  public AbstractListener(Class<? extends EvolutionEvent>... localEventClasses) {
    eventClasses = new LinkedHashSet<>();
      Collections.addAll(eventClasses, localEventClasses);
  }

  @Override
  public Set<Class<? extends EvolutionEvent>> getEventClasses() {
    return eventClasses;
  }    
  
}
