package com.jsls.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import lombok.Data;

@Data
public class Pair<V1, V2> {
    private V1 v1;
    private V2 v2;

    public Pair() {

    }

    public Pair(V1 v1, V2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }

    public static <K, V> List<Pair<K, V>> LoadFrom(Map<K, V> map) {
        List<Pair<K, V>> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                list.add(new Pair<K, V>(entry.getKey(), entry.getValue()));
            }
        }
        return list;
    }
}