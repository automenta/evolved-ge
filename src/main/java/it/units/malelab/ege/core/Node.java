/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import it.units.malelab.ege.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author eric
 */
public class Node<T> extends ArrayList<Node<T>> implements Sequence<T>, Serializable, Iterable<Node<T>> {

    public static final Node EMPTY_TREE = new Node(null);
    final static Node[] EmptyNodeArray = new Node[0];
    public final T content;
    public final List<Node<T>> children = this;
    private Node<T> parent;

    public Node(T content) {
        this.content = content;
    }

    public Node(Node<T> original) {
        this(original != null ? original.content : null);

        if (original != null) {
            //new Node<>(child));
            addAll(original.children);
        }
    }

    @Override
    public Node<T> set(int index, Node<T> element) {
        leaves = null;
        return super.set(index, element);
    }

    public List<Node<T>> leafNodes() {
        if (leaves == null) {
            if (isEmpty()) {
                leaves = List.of(this);
            } else {
                List<Node<T>> l = new ArrayList();
                for (Node n : this) {
                    l.addAll(n.leafNodes());
                }
                leaves = l;
            }
        }

        return leaves;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(content);
        if (!children.isEmpty()) {
            sb.append('{');
            for (Node<T> child: this) {
                sb.append(child).append(',');
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append('}');
        }
        return sb.toString();
    }

    public Iterable<Node<T>> getAncestors() {
        if (parent == null) {
            return List.of();
        }
        return Iterables.concat(List.of(parent), parent.getAncestors());
//        List<Node<T>> ancestors = new ArrayList<>();
//        ancestors.add(parent);
//        ancestors.addAll(parent.getAncestors());
//        return Collections.unmodifiableList(ancestors);
    }

    public Node<T> getParent() {
        return parent;
    }

    public void propagateParentship() {
        for (Node<T> child: children) {
            child.parent = this;
            child.propagateParentship();
        }
    }

    public int depth() {
        int max = 0;
        for (Node<T> child: children) {
            max = Math.max(max, child.depth());
        }
        return max + 1;
    }

    public int nodeSize() {
        int size = 0;
        for (Node<T> child: children) {
            size = size + child.nodeSize();
        }
        return size + 1;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + content.hashCode();
        hash = 53 * hash + super.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Node))
            return false;
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
        final Node<?> other = (Node<?>) obj;
        if (!Objects.equals(this.content, other.content)) {
            return false;
        }

        int ss;
        if ((ss = size())!=other.size()) return false;
        for (int i = 0; i < ss; i++) {
            if (!get(i).equals(other.get(i)))
                return false;
        }

        return true;
    }

    public Sequence<T> leafContents() {
        //final List<Node<T>> leafNodes = leafNodes();
        return Utils.from(Utils.contents(leafNodes()));
    }

    @Override
    public T content(int index) {
        return leafNodes().get(index).content;
    }

    @Override
    public void replace(int index, T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int leaves() {
        return leafNodes().size();
    }

    @Override
    public Sequence<T> clone() {
        return new Node<>(this);
    }





        /**
         * cached leaf nodes
         */
        private List<Node<T>> leaves = List.of(this);

        @Override
        public boolean add(Node<T> tNode) {
            leaves = null;
            return super.add(tNode);
        }

        @Override
        public boolean remove(Object tNode) {
            leaves = null;
            return super.remove(tNode);
        }

        @Override
        public void add(int index, Node<T> element) {
            leaves = null;
            super.add(index, element);
        }



        @Override
        public boolean addAll(int index, Collection<? extends Node<T>> c) {
            leaves = null;
            return super.addAll(index, c);
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            leaves = null;
            super.removeRange(fromIndex, toIndex);
        }

        @Override
        public Node<T> remove(int index) {
            leaves = null;
            return super.remove(index);
        }

        @Override
        public boolean addAll(Collection<? extends Node<T>> c) {
            leaves = null;
            return super.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            leaves = null;
            return super.removeAll(c);
        }

        @Override
        public boolean removeIf(Predicate<? super Node<T>> filter) {
            leaves = null;
            return super.removeIf(filter);
        }

        @Override
        public void clear() {
            leaves = null;
            super.clear();
        }



}
