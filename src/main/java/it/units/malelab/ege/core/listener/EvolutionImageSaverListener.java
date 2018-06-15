/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.listener;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.units.malelab.ege.core.ConstrainedSequence;
import it.units.malelab.ege.core.Individual;
import it.units.malelab.ege.core.fitness.Fitness;
import it.units.malelab.ege.core.listener.event.EvolutionEndEvent;
import it.units.malelab.ege.core.listener.event.EvolutionEvent;
import it.units.malelab.ege.core.listener.event.EvolutionStartEvent;
import it.units.malelab.ege.core.listener.event.GenerationEvent;
import it.units.malelab.ege.ge.mapper.StandardGEMapper;
import it.units.malelab.ege.util.Utils;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 *
 * @author eric
 */
public class EvolutionImageSaverListener<G extends ConstrainedSequence, T, F extends Fitness> extends AbstractListener<G, T, F> implements WithConstants {

  public enum ImageType {
    DIVERSITY,
    USAGE,
    DU,
    BEST_USAGE
  }

  private final Map<String, Object> constants;
  private final String basePath;
  private final EnumSet<ImageType> types;

  private final List<double[]> evolutionDiversities;
  private final List<double[]> evolutionUsages;
  private final List<double[]> evolutionBestUsages;

  public EvolutionImageSaverListener(
          Map<String, Object> constants,
          String basePath, ImageType... types) {
    super(EvolutionStartEvent.class, GenerationEvent.class, EvolutionEndEvent.class);
    this.constants = new LinkedHashMap<>(constants);
    this.basePath = basePath;
    evolutionDiversities = new ArrayList<>();
    evolutionUsages = new ArrayList<>();
    evolutionBestUsages = new ArrayList<>();
    this.types = EnumSet.copyOf(Arrays.asList(types));
  }

  @Override
  public void listen(EvolutionEvent<G, T, F> event) {
    if (types.isEmpty()) {
      return;
    }
    List<List<Individual<G, T, F>>> rankedPopulation = ((GenerationEvent) event).getRankedPopulation();
    //update best usages
    Individual<G, T, F> best = rankedPopulation.get(0).get(0);
    if (types.contains(ImageType.BEST_USAGE)) {
      double[] bestUsages = new double[best.genotype.leaves()];
      int[] bitUsages = (int[]) best.otherInfo.get(StandardGEMapper.BIT_USAGES_INDEX_NAME);
      if (bitUsages != null) {
        double maxUsage = 0;
        for (int bitUsage : bitUsages) {
          maxUsage = Math.max(maxUsage, bitUsage);
        }
        for (int i = 0; i < Math.min(bitUsages.length, bestUsages.length); i++) {
          bestUsages[i] = bitUsages[i] / maxUsage;
        }
      }
      evolutionBestUsages.add(bestUsages);
    }
    //update diversities
    if (types.contains(ImageType.DIVERSITY) || types.contains(ImageType.DU)) {
      Set[] domains = new Set[best.genotype.leaves()];
      Multiset[] symbols = new Multiset[best.genotype.leaves()];
      for (int i = 0; i < symbols.length; i++) {
        symbols[i] = HashMultiset.create();
        domains[i] = new LinkedHashSet();
      }
      double[] counts = new double[best.genotype.leaves()];
      for (List<Individual<G, T, F>> rank : rankedPopulation) {
        for (Individual<G, T, F> individual : rank) {
          for (int i = 0; i < Math.min(best.genotype.leaves(), individual.genotype.leaves()); i++) {
            counts[i] = counts[i] + 1;
            symbols[i].add(individual.genotype.content(i));
            domains[i].addAll(individual.genotype.domain(i));
          }
        }
      }
      double[] diversities = new double[best.genotype.leaves()];
      for (int i = 0; i < symbols.length; i++) {
        diversities[i] = Utils.multisetDiversity(symbols[i], domains[i]);
      }
      evolutionDiversities.add(diversities);
    }
    //update usages
    if (types.contains(ImageType.USAGE) || types.contains(ImageType.DU)) {
      double[] usages = new double[best.genotype.leaves()];
      double count = 0;
      for (List<Individual<G, T, F>> rank : rankedPopulation) {
        for (Individual<G, T, F> individual : rank) {
          int[] bitUsages = (int[]) individual.otherInfo.get(StandardGEMapper.BIT_USAGES_INDEX_NAME);
          if (bitUsages != null) {
            double maxUsage = 0;
            for (int bitUsage : bitUsages) {
              maxUsage = Math.max(maxUsage, bitUsage);
            }
            for (int i = 0; i < Math.min(bitUsages.length, usages.length); i++) {
              usages[i] = usages[i] + bitUsages[i] / maxUsage;
            }
            count = count + 1;
          }
        }
      }
      if (count > 0) {
        for (int i = 0; i < usages.length; i++) {
          usages[i] = usages[i] / count;
        }
      }
      evolutionUsages.add(usages);
    }
    if (event instanceof EvolutionEndEvent) {
      if (basePath != null) {
        //save
        String baseFileName = "";
        for (Object value : constants.values()) {
          baseFileName = baseFileName + value + '-';
        }
        if (types.contains(ImageType.DIVERSITY)) {
          saveCSV(basePath + File.separator + baseFileName + "diversitiy.csv", toArray(evolutionDiversities));
          saveImage(basePath + File.separator + baseFileName + "diversity.png", toArray(evolutionDiversities));
        }
        if (types.contains(ImageType.USAGE)) {
          saveImage(basePath + File.separator + baseFileName + "usage.png", toArray(evolutionUsages));
          saveCSV(basePath + File.separator + baseFileName + "usage.csv", toArray(evolutionUsages));
        }
        if (types.contains(ImageType.DU)) {
          saveImage(basePath + File.separator + baseFileName + "diversity_usage.png", toArray(evolutionDiversities), toArray(evolutionUsages));
        }
        if (types.contains(ImageType.BEST_USAGE)) {
          saveImage(basePath + File.separator + baseFileName + "bestUsage.png", toArray(evolutionBestUsages));
          saveCSV(basePath + File.separator + baseFileName + "bestUsage.csv", toArray(evolutionBestUsages));
        }
      }
    }
    if (event instanceof EvolutionStartEvent) {
      //clear
      evolutionBestUsages.clear();
      evolutionUsages.clear();
      evolutionDiversities.clear();

    }
  }

  public double[][][] getLastEvolutionData() {
    return new double[][][]{toArray(evolutionDiversities), toArray(evolutionUsages)};
  }

  @Override
  public void updateConstants(Map<String, Object> newConstants
  ) {
    for (String key : constants.keySet()) {
      if (newConstants.containsKey(key)) {
        constants.put(key, newConstants.get(key));
      }
    }
  }

  private static double[][] toArray(List<double[]> list) {
    double[][] data = new double[list.size()][];
    for (int i = 0; i < list.size(); i++) {
      data[i] = list.get(i);
    }
    return data;
  }

  private static void saveCSV(String fileName, double[][] data) {
    try (PrintStream ps = new PrintStream(fileName)) {
      for (double[] aData: data) {
        for (int i = 0; i < aData.length; i++) {
          ps.printf("%6.4f", aData[i]);
          if (i == aData.length - 1) {
            ps.println();
          } else {
            ps.print(";");
          }
        }
      }
    } catch (FileNotFoundException ex) {
      System.err.printf("Cannot save file \"%s\": %s", fileName, ex.getMessage());
    }
  }

  private static void saveImage(String fileName, double[][]... data) {
    BufferedImage bi = new BufferedImage(data[0][0].length, data[0].length, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < data[0].length; y++) {
      for (int x = 0; x < data[0][y].length; x++) {
        Color color;
        if (data.length == 1) {
          color = new Color((float) data[0][y][x], (float) data[0][y][x], (float) data[0][y][x], 1);
        } else {
          color = new Color(
                  (float) data[0][y][x],
                  (float) data[1][y][x],
                  data.length >= 3 ? (float) data[2][y][x] : 0,
                  data.length >= 4 ? (float) data[3][y][x] : 1
          );
        }
        bi.setRGB(x, y, color.getRGB());
      }
    }
    try {
      ImageIO.write(bi, "PNG", new File(fileName));
    } catch (IOException ex) {
      System.err.printf("Cannot save file \"%s\": %s", fileName, ex.getMessage());
    }
  }

  public static void printLegend(String fileName) throws IOException {
    double n = 200;
    double d = 5;
    BufferedImage bi = new BufferedImage((int) n, (int) n, BufferedImage.TYPE_INT_ARGB);
    for (double x = 0; x < n; x++) {
      for (double y = 0; y < n; y++) {
        bi.setRGB((int) x, (int) y, new Color((float) (Math.floor(x / n * d) / d), (float) (Math.floor(y / n * d) / d), 0, 1).getRGB());
      }
    }
    ImageIO.write(bi, "PNG", new File(fileName));
  }

}
