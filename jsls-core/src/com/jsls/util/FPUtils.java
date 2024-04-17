package org.jsls.util;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 函数式编程工具类
 * 
 * @author zhangwei
 * @since 2023-08-25
 */
public class FPUtils {
    /**
     * 
     * @param <T>
     * @param coll
     * @return
     */
    public static <T> Stream<T> flow(Collection<T> coll) {
        if (coll != null) {
            return coll.stream();
        }
        return Stream.empty();
    }

    /**
     * 函数柯里化-两个参数的函数
     * 
     * @param <E>
     * @param <D>
     * @param <A>
     * @param fn
     * @param a
     * @return
     */
    public static <E, D, A> Function<E, D> curry(BiFunction<E, A, D> fn, A a) {
        return (E e) -> {
            return fn.apply(e, a);
        };
    }

    /**
     * 函数柯里化-两个参数的函数
     * 
     * @param <E>
     * @param <D>
     * @param <A>
     * @param fn
     * @param a
     * @return
     */
    public static <E, D, A> Consumer<E> curry(BiConsumer<E, A> fn, A a) {
        return (E e) -> {
            fn.accept(e, a);
        };
    }

    /**
     * 函数柯里化-三个参数的函数
     * 
     * @param <E>
     * @param <D>
     * @param <A>
     * @param <B>
     * @param fn
     * @param a
     * @param b
     * @return
     */
    public static <E, D, A, B> Function<E, D> curry(TreFunction<E, A, B, D> fn, A a, B b) {
        return (E e) -> {
            return fn.apply(e, a, b);
        };
    }

    /**
     * 函数柯里化-三个参数的函数
     * 
     * @param <E>
     * @param <A>
     * @param <B>
     * @param fn
     * @param a
     * @param b
     * @return
     */
    public static <E, A, B> Consumer<E> curry(TreConsumer<E, A, B> fn, A a, B b) {
        return (E e) -> {
            fn.accept(e, a, b);
        };
    }

    /**
     * 函数柯里化-三个参数的函数为二个参数函数
     * 
     * @param <E>
     * @param <A>
     * @param <B>
     * @param fn
     * @param a
     * @param b
     * @return
     */
    public static <E, A, B> BiConsumer<E, A> curry(TreConsumer<E, A, B> fn, B b) {
        return (E e, A a) -> {
            fn.accept(e, a, b);
        };
    }

    /**
     * 函数柯里化-四个参数的函数
     * 
     * @param <E>
     * @param <D>
     * @param <A>
     * @param <B>
     * @param <C>
     * @param fn
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <E, D, A, B, C> Function<E, D> curry(FourFunction<E, A, B, C, D> fn, A a, B b, C c) {
        return (E e) -> {
            return fn.apply(e, a, b, c);
        };
    }

    /**
     * 函数柯里化-四个参数的函数
     * 
     * @param <E>
     * @param <A>
     * @param <B>
     * @param <C>
     * @param fn
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <E, A, B, C> Consumer<E> curry(FourConsumer<E, A, B, C> fn, A a, B b, C c) {
        return (E e) -> {
            fn.accept(e, a, b, c);
        };
    }

    @FunctionalInterface
    public static interface TreConsumer<T, A, B> {
        void accept(T t, A a, B b);

        default TreConsumer<T, A, B> andThen(TreConsumer<? super T, ? super A, ? super B> after) {
            Objects.requireNonNull(after);
            return (l, a, b) -> {
                accept(l, a, b);
                after.accept(l, a, b);
            };
        }
    }

    @FunctionalInterface
    public static interface FourConsumer<T, A, B, C> {
        void accept(T t, A a, B b, C c);

        default FourConsumer<T, A, B, C> andThen(FourConsumer<? super T, ? super A, ? super B, ? super C> after) {
            Objects.requireNonNull(after);
            return (l, a, b, c) -> {
                accept(l, a, b, c);
                after.accept(l, a, b, c);
            };
        }
    }

    @FunctionalInterface
    public static interface TreFunction<T, A, B, R> {
        R apply(T t, A a, B b);

        default <V> TreFunction<T, A, B, V> andThen(Function<? super R, ? extends V> after) {
            Objects.requireNonNull(after);
            return (T t, A a, B b) -> after.apply(apply(t, a, b));
        }
    }

    @FunctionalInterface
    public static interface FourFunction<T, A, B, C, R> {
        R apply(T t, A a, B b, C c);

        default <V> FourFunction<T, A, B, C, V> andThen(Function<? super R, ? extends V> after) {
            Objects.requireNonNull(after);
            return (T t, A a, B b, C c) -> after.apply(apply(t, a, b, c));
        }
    }
}
