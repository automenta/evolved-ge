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
 * stores subnodes as elements of the List it extends
 */
public class Node<T> extends ArrayList<Node<T>> implements Sequence<T>, Serializable, Iterable<Node<T>> {

    public static final Node EMPTY_TREE = new Node(null) {
        @Override
        public boolean add(Node node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, Node element) {
            throw new UnsupportedOperationException();
        }
    };

    public final T content;

    private Node<T> parent;

    transient private int hash = 0;
    transient private List<Node<T>> leaves = List.of(this);


    public Node(T content) {
        this.content = content;
    }

    public Node(Node<T> original) {
        this(original != null ? original.content : null);

        if (original != null) {
            addAll(original);
        }
    }

    private void changed() {
        leaves = null;
        hash = 0;
    }
    
    @Override
    public Node<T> set(int index, Node<T> element) {
        if (element == this)
            throw new RuntimeException();
        if (!get(index).equals(element)) {
            changed();
            return super.set(index, element);
        }
        return element;
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
        if (!isEmpty()) {
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
        for (int i = 0, childrenSize = size(); i < childrenSize; i++) {
            Node<T> child = get(i);
            if (child.parent != this) {
                child.parent = this;
                child.propagateParentship();
            }
        }
    }

    public int depth() {
        int max = 0;
        for (int i = 0, childrenSize = size(); i < childrenSize; i++) {
            Node<T> child = get(i);
            max = Math.max(max, child.depth());
        }
        return max + 1;
    }

    public int nodeSize() {
        int size = 0;
        for (int i = 0, childrenSize = size(); i < childrenSize; i++) {
            Node<T> child = get(i);
            size = size + child.nodeSize();
        }
        return size + 1;
    }

    @Override
    public final int hashCode() {
        int h = hash;
        if (h == 0) {
            int hash = 53 + (content!=null ? content.hashCode() : 0);
            int s = size();
            for (int i = 0; i < s; i++) {
                hash = 53 * hash + get(i).hashCode();
            }
            if (hash == 0) hash =1;
            return this.hash = hash;
        }
        return h;
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
        if (hashCode()!=other.hashCode())
            return false;
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





        @Override
        public boolean add(Node<T> tNode) {
            if (tNode == this || (tNode.parent!=null && tNode.parent!=this))
                tNode = new Node(tNode); //clone
            changed();
            return super.add(tNode);
        }

        @Override
        public boolean remove(Object tNode) {
            if (super.remove(tNode)) {
                changed();
                return true;
            }
            return false;
        }

        @Override
        public void add(int index, Node<T> element) {
            if (element == this)
                throw new RuntimeException();
            changed();
            super.add(index, element);
        }



        @Override
        public boolean addAll(int index, Collection<? extends Node<T>> c) {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            changed();
            super.removeRange(fromIndex, toIndex);
        }

        @Override
        public Node<T> remove(int index) {
            changed();
            return super.remove(index);
        }

        @Override
        public boolean addAll(Collection<? extends Node<T>> c) {
            if (c == this) {
                int s = size();
                for (int i = 0; i < s; i++) {
                    add(get(i));
                }
                changed();
                return true;
            }
            int sizeBefore = 0;
            c.forEach(this::add);
            if (size()> sizeBefore) {
                changed();
                return true;
            } return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            if (c == this) {
                clear();
                return true;
            }
            if (super.removeAll(c)) {
                changed();
                return true;
            }
            return false;
        }

        @Override
        public boolean removeIf(Predicate<? super Node<T>> filter) {

            if (super.removeIf(filter)) {
                changed();
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            if (size() > 0) {
                super.clear();
                changed();
            }
        }


    public void clearParents() {
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            Node n = this.get(i);
            n.parent = null;
        }
    }
}
