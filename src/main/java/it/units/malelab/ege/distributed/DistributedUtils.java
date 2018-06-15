/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.distributed;

import it.units.malelab.ege.distributed.master.Master;
import it.units.malelab.ege.core.listener.collector.Collector;
import it.units.malelab.ege.util.Pair;
import java.io.PrintStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author eric
 */
public class DistributedUtils {

  private final static Logger L = Logger.getLogger(DistributedUtils.class.getName());

  public static byte[] encrypt(String s, String keyString) {
    return cipher(s.getBytes(), keyString, Cipher.ENCRYPT_MODE);
  }

  public static String decrypt(byte[] bytes, String keyString) {
    return new String(cipher(bytes, keyString, Cipher.DECRYPT_MODE));
  }

  private static byte[] cipher(byte[] bytes, String keyString, int mode) {
    try {
      Cipher c = Cipher.getInstance("AES");
      byte[] keyBytes = new byte[16];
      byte[] shortKeyBytes = Base64.getEncoder().encode(keyString.getBytes());
      System.arraycopy(shortKeyBytes, 0, keyBytes, 0, shortKeyBytes.length);
      Key key = new SecretKeySpec(keyBytes, "AES");
      c.init(mode, key);
      return c.doFinal(bytes);
    } catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException ex) {
      L.log(Level.SEVERE, String.format("Cannot encrypt: %s", ex), ex);
    } catch (NoSuchPaddingException ex) {
      L.log(Level.SEVERE, String.format("Cannot encrypt: %s", ex), ex);
      //} catch (IOException ex) {
      L.log(Level.SEVERE, String.format("Cannot encrypt: %s", ex), ex);
    }
    return bytes;
  }

  public static String reverse(String s) {
    StringBuilder sb = new StringBuilder();
    sb.append(s);
    sb.reverse();
    return sb.toString();
  }

  public static List<String> jobKeys(Job job) {
      List<String> keys = new ArrayList<>(job.getKeys().keySet());
    keys.add(Master.CLIENT_NAME);
    keys.add(Master.JOB_ID_NAME);
    keys.add(Master.GENERATION_NAME);
    keys.add(Master.LOCAL_TIME_NAME);
    for (Collector collector : (List<Collector>) job.getCollectors()) {
      keys.addAll(collector.getFormattedNames().keySet());
    }
    return keys;
  }

  public static void writeHeader(PrintStream ps, List<String> keys) {
    for (int i = 0; i < keys.size() - 1; i++) {
      ps.print(keys.get(i) + ';');
    }
    ps.println(keys.get(keys.size() - 1));
  }

  public synchronized static void writeData(PrintStream ps, Job job, Iterable<Map<String, Object>> data) {
    List<String> keys = jobKeys(job);
    for (Map<String, Object> dataItem : data) {
      Map<String, Object> allData = new HashMap<>(dataItem);
      allData.putAll(job.getKeys());
      for (int i = 0; i < keys.size() - 1; i++) {
        ps.print(allData.get(keys.get(i)) + ";");
      }
      ps.println(allData.get(keys.get(keys.size() - 1)));
    }
  }

  public static <K, V> Pair<K, V> getAny(Map<K, Future<V>> futures, long millis) {
    while (true) {
      if (futures.isEmpty()) {
        return null;
      }
      for (Map.Entry<K, Future<V>> entry : futures.entrySet()) {
        try {
          V v = entry.getValue().get(millis, TimeUnit.MILLISECONDS);
          futures.remove(entry.getKey());
          return new Pair<>(entry.getKey(), v);
        } catch (InterruptedException | TimeoutException | ExecutionException ex) {
          //ignore
        }
      }
    }
  }

}
