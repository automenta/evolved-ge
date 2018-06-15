/* MIT License
 *
 * Copyright (c) 2017 Mateusz Pawlik
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package apted.costmodel;

import apted.node.Node;

/**
 * This is a unit-nost model defined on string labels.
 *
 * @see CostModel
 * @see String
 */
 // TODO: Use a label dictionary to encode string labels with integers for
 //       faster rename cost computation.
public class StringUnitCostModel implements CostModel<String> {

  /**
   * Calculates the cost of deleting a node.
   *
   * @param n a node considered to be deleted.
   * @return {@code 1} - a fixed cost of deleting a node.
   */
  public float del(Node<String> n) {
    return 1.0f;
  }

  /**
   * Calculates the cost of inserting a node.
   *
   * @param n a node considered to be inserted.
   * @return {@code 1} - a fixed cost of inserting a node.
   */
  public float ins(Node<String> n) {
    return 1.0f;
  }

  /**
   * Calculates the cost of renaming the label of the source node to the label
   * of the destination node.
   *
   * @param n1 a source node for rename.
   * @param n2 a destination node for rename.
   * @return {@code 1} if labels of renamed nodes are equal, and {@code 0} otherwise.
   */
  public float ren(Node<String> n1, Node<String> n2) {
    return (n1==n2) ? 0 :
            ((n1.getNodeData().equals(n2.getNodeData())) ? 0.0f : 1.0f);
  }
}
