package com.rahul.security.cam.conversion.utils;

import java.util.function.Predicate;

@FunctionalInterface
public interface CheckedPredicate<T> extends Predicate<T> {
    /**
     * Evaluates this predicate on the given argument.
     *
     * @param t the input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean filter(T t) throws Exception;

    @Override
    default boolean test(T t) {
        try {
            return filter(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
