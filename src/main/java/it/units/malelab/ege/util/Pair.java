/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.util;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author eric
 */
public class Pair<F, S> implements Serializable {

    public final F first;
    public final S second;
    public final int hash;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
        int hash = 5;
        hash = 83 * hash + this.first.hashCode();
        hash = 83 * hash + this.second.hashCode();
        this.hash = hash;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Pair)) return false;

        final Pair other = (Pair) obj;
        if (hash!=other.hash) return false;

        return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second);
    }

    @Override
    public String toString() {
        return "<" + first + ", " + second + '>';
    }

}
