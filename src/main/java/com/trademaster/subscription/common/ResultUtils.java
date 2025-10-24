package com.trademaster.subscription.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Result Utility Methods
 * MANDATORY: Single Responsibility - Result utility operations only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Provides utility methods for Result pattern operations.
 *
 * @author TradeMaster Development Team
 */
public final class ResultUtils {

    private ResultUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Combine two Results into one with a binary function
     */
    public static <T, U, V, E> Result<V, E> combine(
            Result<T, E> first,
            Result<U, E> second,
            BiFunction<T, U, V> combiner) {
        return first.flatMap(t -> second.map(u -> combiner.apply(t, u)));
    }

    /**
     * Sequence a list of Results into a Result of list
     * Returns first failure encountered, or success with all values
     * MANDATORY: Rule #3 - No loops, using Stream API
     */
    public static <T, E> Result<List<T>, E> sequence(List<Result<T, E>> results) {
        return results.stream()
            .filter(Result::isFailure)
            .findFirst()
            .map(failure -> {
                @SuppressWarnings("unchecked")
                Result<List<T>, E> failureResult =
                    (Result<List<T>, E>) failure.mapError(Function.identity());
                return failureResult;
            })
            .orElseGet(() -> Result.success(
                results.stream()
                    .map(Result::getValue)
                    .collect(Collectors.toList())
            ));
    }
}
