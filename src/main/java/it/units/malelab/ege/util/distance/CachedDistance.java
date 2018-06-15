/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.util.distance;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

/**
 *
 * @author eric
 */
public class CachedDistance<T> implements Distance<T> {

  private transient LoadingCache<List<T>, Double> cache;
  private final Distance<T> distance;

  private static final int CACHE_SIZE = 16*1024;

  public CachedDistance(Distance<T> distance) {
    this.distance = distance;
    buildCache();
  }

  @Override
  public double d(T t1, T t2) {
    return cache.getUnchecked(List.of(t1, t2));
  }

  private void buildCache() {
    cache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build(new CacheLoader<>() {
      @Override
      public Double load(List<T> ts) {
        T a = ts.get(0);
        T b = ts.get(1);
        double d = distance.d(a, b);
        cache.put(List.of(b, a), d); //put pair in the other order
        return d;
      }
    });
  }

  private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
    inputStream.defaultReadObject();
    buildCache();
  }

}
