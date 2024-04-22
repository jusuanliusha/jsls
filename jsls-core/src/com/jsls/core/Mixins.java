package com.jsls.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jack.common.util.FPUtils;
import org.jack.common.util.FPUtils.TreConsumer;
import org.jack.common.util.ValueUtils;
import org.springframework.util.CollectionUtils;

/**
 * 混入类
 */
public class Mixins<K, D> {
    private Map<K, D> destMap;

    public Mixins(Collection<D> list, Function<D, K> kfn) {
        destMap = ValueUtils.map(list, kfn);
    }

    public Mixins(Map<K, D> dest) {
        destMap = dest;
    }

    public static <K1, D1> Mixins<K1, D1> of(Collection<D1> list, Function<D1, K1> kfn) {
        return new Mixins<K1, D1>(list, kfn);
    }

    public static <K1, D1> Mixins<K1, D1> of(Map<K1, D1> dest) {
        return new Mixins<K1, D1>(dest);
    }

    /**
     * 混入单条数据
     * 
     * @param <M>
     * @param fetcher
     * @param mixfn
     * @param mkfn
     * @return
     */
    public <M> Mixins<K, D> apply(Function<Collection<K>, List<M>> fetcher, BiConsumer<D, M> mixfn,
            Function<M, K> mkfn) {
        mappingMixins(destMap, fetcher, mixfn, mkfn);
        return this;
    }

    /**
     * 混入单条数据
     * 
     * @param <M>
     * @param fetcher
     * @param mixfn
     * @return
     */
    public <M extends Keyable<K>> Mixins<K, D> apply(Function<Collection<K>, List<M>> fetcher, BiConsumer<D, M> mixfn) {
        mappingMixins(destMap, fetcher, mixfn, M::useKey);
        return this;
    }

    /**
     * 混入多条数据
     * 
     * @param <M>
     * @param fetcher
     * @param mixfn
     * @param mkfn
     * @return
     */
    public <M> Mixins<K, D> applyList(Function<Collection<K>, List<M>> fetcher, BiConsumer<D, List<M>> mixfn,
            Function<M, K> mkfn) {
        mappingMixinsList(destMap, fetcher, mixfn, mkfn);
        return this;
    }

    /**
     * 混入多条数据
     * 
     * @param <M>
     * @param fetcher
     * @param mixfn
     * @return
     */
    public <M extends Keyable<K>> Mixins<K, D> applyList(Function<Collection<K>, List<M>> fetcher,
            BiConsumer<D, List<M>> mixfn) {
        mappingMixinsList(destMap, fetcher, mixfn, M::useKey);
        return this;
    }

    /**
     * 每一项依次混入(ext,a)
     * 
     * @param <D>
     * @param <E>
     * @param <A>
     * @param dest
     * @param ext
     * @param a
     * @param mix
     */
    public static <D, E, A> void apply(Collection<D> dest, E ext, A a, TreConsumer<D, E, A> mix) {
        apply(dest, ext, FPUtils.curry(mix, a));
    }

    /**
     * 每一项依次混入ext
     * 
     * @param <D>
     * @param <E>
     * @param dest
     * @param ext
     * @param mix
     */
    public static <D, E> void apply(Collection<D> dest, E ext, BiConsumer<D, E> mix) {
        if (CollectionUtils.isEmpty(dest)) {
            return;
        }
        for (D item : dest) {
            if (item != null) {
                mix.accept(item, ext);
            }
        }
    }

    /**
     * 映射匹配然后混合
     * 
     * @param <K>
     * @param <D>
     * @param <E>
     * @param dest
     * @param fetcher
     * @param mix
     */
    public static <K, D, E> void mappingMixins(Map<K, D> dest, Function<Collection<K>, List<E>> fetcher,
            BiConsumer<D, E> mix, Function<E, K> ekfn) {
        if (CollectionUtils.isEmpty(dest)) {
            return;
        }
        List<E> list = fetcher.apply(dest.keySet());
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (E e : list) {
            D d = dest.get(ekfn.apply(e));
            mix.accept(d, e);
        }
    }

    /**
     * 映射匹配然后混合
     * 
     * @param <K>
     * @param <D>
     * @param <E>
     * @param dest
     * @param fetcher
     * @param mix
     */
    public static <K, D, E> void mappingMixinsList(Map<K, D> dest, Function<Collection<K>, List<E>> fetcher,
            BiConsumer<D, List<E>> mix, Function<E, K> ekfn) {
        if (CollectionUtils.isEmpty(dest)) {
            return;
        }
        List<E> list = fetcher.apply(dest.keySet());
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Map<K, List<E>> mixMap = ValueUtils.mapList(list, ekfn);
        for (Map.Entry<K, List<E>> entry : mixMap.entrySet()) {
            D d = dest.get(entry.getKey());
            mix.accept(d, entry.getValue());
        }
    }
}
