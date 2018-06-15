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
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

/**
 * @author eric
 */
public class Node<T> implements Sequence<T>, Serializable {

    public static final Node EMPTY_TREE = new Node(null);

    public final T content;
    public final List<Node<T>> children = new ArrayList<>();
    private Node<T> parent;

    public Node(T content) {
        this.content = content;
    }

    public Node(Node<T> original) {
        this(original != null ? original.content : null);

        if (original!=null) {
            //new Node<>(child));
            children.addAll(original.children);
        }
    }


    public Iterable<Node<T>> leafNodes() {
        if (children.isEmpty()) {
            return List.of();
        }
//        List<Node<T>> childContents = new ArrayList<>();
//        for (Node<T> child: children) {
//            childContents.addAll(child.leafNodes());
//        }
//        return childContents;

        return ()->children.stream().flatMap(c ->
                StreamSupport.stream(c.leafNodes().spliterator(), false))
                .iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(content);
        if (!children.isEmpty()) {
            sb.append('{');
            for (Node<T> child: children) {
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
        hash = 53 * hash + Objects.hashCode(this.content);
        hash = 53 * hash + Objects.hashCode(this.children);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Node<?> other = (Node<?>) obj;
        if (!Objects.equals(this.content, other.content)) {
            return false;
        }
        return Objects.equals(this.children, other.children);
    }

    public Sequence<T> leafContents() {
        //final List<Node<T>> leafNodes = leafNodes();
        return Utils.from(Utils.contents(leafNodes()));
    }

    @Override
    public T get(int index) {
        return Iterables.get(leafNodes(), index).content;
    }

    @Override
    public int size() {
        return Iterables.size(leafNodes());
    }

    @Override
    public Sequence<T> clone() {
        return new Node<>(this);
    }

    @Override
    public void set(int index, T t) {
        throw new UnsupportedOperationException("Set not supported on trees.");
    }

    public List<Node<T>> leafNodesList() {
        return Lists.newArrayList(leafNodes());
    }
}
