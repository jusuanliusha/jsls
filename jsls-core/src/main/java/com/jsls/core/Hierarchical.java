package com.jsls.core;

public interface Hierarchical<T extends Hierarchical<T>> {
    T getParent();
}
