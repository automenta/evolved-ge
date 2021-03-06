/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.ge.mapper;

import it.units.malelab.ege.core.mapper.AbstractMapper;
import it.units.malelab.ege.core.mapper.MappingException;
import it.units.malelab.ege.ge.genotype.BitsGenotype;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.Grammar;
import static it.units.malelab.ege.ge.mapper.StandardGEMapper.BIT_USAGES_INDEX_NAME;
import java.util.List;
import java.util.Map;

/**
 *
 * @author eric
 */
public class BreathFirstMapper<T> extends AbstractMapper<BitsGenotype, T> {

  private final int codonLenght;
  private final int maxWraps;

  public BreathFirstMapper(int codonLenght, int maxWraps, Grammar<T> grammar) {
    super(grammar);
    this.codonLenght = codonLenght;
    this.maxWraps = maxWraps;
  }

  private static class EnhancedSymbol<T> {

    private final T symbol;
    private final int depth;

    EnhancedSymbol(T symbol, int depth) {
      this.symbol = symbol;
      this.depth = depth;
    }

    T getSymbol() {
      return symbol;
    }

    int getDepth() {
      return depth;
    }

    @Override
    public String toString() {
      return symbol+":"+depth;
    }

  }

  @Override
  public Node<T> map(BitsGenotype genotype, Map<String, Object> report) throws MappingException {
    int[] bitUsages = new int[genotype.leaves()];
    if (genotype.leaves()<codonLenght) {
      throw new MappingException(String.format("Short genotype (%d<%d)", genotype.leaves(), codonLenght));
    }
    Node<EnhancedSymbol<T>> enhancedTree = new Node<>(new EnhancedSymbol<>(grammar.getStartingSymbol(), 0));
    int currentCodonIndex = 0;
    int wraps = 0;
    while (true) {
      int minDepth = Integer.MAX_VALUE;
      Node<EnhancedSymbol<T>> nodeToBeReplaced = null;
      for (Node<EnhancedSymbol<T>> node : enhancedTree.leafNodes()) {
          if (((Map<T, List<List<T>>>) grammar).keySet().contains(node.content.getSymbol())&&(node.content.getDepth()<minDepth)) {
          nodeToBeReplaced = node;
              minDepth = node.content.getDepth();
        }
      }
      if (nodeToBeReplaced==null) {
        break;
      }
      //get codon index and option
      if ((currentCodonIndex + 1) * codonLenght > genotype.leaves()) {
        wraps = wraps + 1;
        currentCodonIndex = 0;
        if (wraps > maxWraps) {
          throw new MappingException(String.format("Too many wraps (%d>%d)", wraps, maxWraps));
        }
      }
        List<List<T>> options = ((Map<T, List<List<T>>>) grammar).get(nodeToBeReplaced.content.getSymbol());
      int optionIndex = genotype.slice(currentCodonIndex * codonLenght, (currentCodonIndex + 1) * codonLenght).toInt() % options.size();
      //update usages
      for (int i = currentCodonIndex*codonLenght; i<(currentCodonIndex+1)*codonLenght; i++) {
        bitUsages[i] = bitUsages[i]+1;
      }
      //add children
      for (T t : options.get(optionIndex)) {
          Node<EnhancedSymbol<T>> newChild = new Node<>(new EnhancedSymbol<>(t, nodeToBeReplaced.content.getDepth() + 1));
        nodeToBeReplaced.add(newChild);
      }
      currentCodonIndex = currentCodonIndex+1;
    }
    report.put(BIT_USAGES_INDEX_NAME, bitUsages);
    //convert
    return extractFromEnhanced(enhancedTree);
  }
  
  private Node<T> extractFromEnhanced(Node<EnhancedSymbol<T>> enhancedNode) {
      Node<T> node = new Node<>(enhancedNode.content.getSymbol());
    for (Node<EnhancedSymbol<T>> enhancedChild : enhancedNode) {
      node.add(extractFromEnhanced(enhancedChild));
    }
    return node;
  }

  @Override
  public String toString() {
    return "BreathFirstMapper{" + "codonLenght=" + codonLenght + ", maxWraps=" + maxWraps + '}';
  }

}
