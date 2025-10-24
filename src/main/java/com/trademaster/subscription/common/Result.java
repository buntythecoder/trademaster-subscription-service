package com.trademaster.subscription.common;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Result Type for Railway Programming Pattern
 * MANDATORY: Single Responsibility - Result contract only
 * MANDATORY: Rule #5 - <200 lines per class
 * MANDATORY: Error Handling Patterns - Rule #11
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Pattern Matching - Rule #14
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public sealed interface Result<T, E> permits ResultSuccess, ResultFailure {

    /**
     * Create successful result
     */
    static <T, E> Result<T, E> success(T value) {
        return new ResultSuccess<>(value);
    }

    /**
     * Create failure result
     */
    static <T, E> Result<T, E> failure(E error) {
        return new ResultFailure<>(error);
    }

    /**
     * Try to execute operation and wrap result
     */
    static <T> Result<T, Exception> tryExecute(Supplier<T> operation) {
        try {
            return success(operation.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Try to execute operation that may throw checked exceptions
     */
    static <T> Result<T, Exception> tryExecuteChecked(CheckedSupplier<T> operation) {
        try {
            return success(operation.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Functional interface for operations that may throw checked exceptions
     */
    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Check if result is successful
     */
    boolean isSuccess();

    /**
     * Check if result is failure
     */
    default boolean isFailure() {
        return !isSuccess();
    }

    /**
     * Get value if success, otherwise throw
     */
    T getValue();

    /**
     * Get error if failure, otherwise throw
     */
    E getError();

    /**
     * Get value as Optional
     */
    Optional<T> getValueOptional();

    /**
     * Get error as Optional
     */
    Optional<E> getErrorOptional();

    /**
     * Map success value to new type
     */
    <U> Result<U, E> map(Function<T, U> mapper);

    /**
     * FlatMap for chaining operations
     */
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);

    /**
     * Map error to new type
     */
    <F> Result<T, F> mapError(Function<E, F> errorMapper);

    /**
     * Filter result with predicate
     */
    Result<T, E> filter(Predicate<T> predicate, E errorOnFalse);

    /**
     * Execute side effect on success
     */
    Result<T, E> onSuccess(Consumer<T> action);

    /**
     * Execute side effect on failure
     */
    Result<T, E> onFailure(Consumer<E> action);

    /**
     * Recover from failure
     */
    Result<T, E> recover(Function<E, T> recovery);

    /**
     * Recover from failure with another Result
     */
    Result<T, E> recoverWith(Function<E, Result<T, E>> recovery);

    /**
     * Pattern matching - execute different functions based on success/failure
     */
    <U> U match(Function<T, U> onSuccess, Function<E, U> onFailure);

    /**
     * Convert to Optional, losing error information
     */
    Optional<T> toOptional();
}
