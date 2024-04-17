package org.jsls.core;

import java.util.function.BiFunction;

public class Holder<V,E> {
    private V v;
    private BiFunction<V,E,V> fn;
    public Holder(V v,BiFunction<V,E,V> fn){
        this.v=v;
        this.fn=fn;
    }
    public V apply(E e){
        this.v=fn.apply(this.v,e);
        return this.v;
    }
    public V getV() {
        return v;
    }
    public synchronized V applyThreadSafe(E e){
        return apply(e);
    }
    public synchronized V getThreadSafeV() {
        return getV();
    }
}