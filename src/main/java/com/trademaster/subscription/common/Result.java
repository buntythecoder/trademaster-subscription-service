package com.trademaster.subscription.common;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Result Type for Railway Programming Pattern
 * 
 * MANDATORY: Error Handling Patterns - Rule #11
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Pattern Matching - Rule #14
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    /**
     * Create successful result
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    /**
     * Create failure result
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
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
    
    /**
     * Success case implementation
     */
    record Success<T, E>(T value) implements Result<T, E> {
        @Override
        public boolean isSuccess() {
            return true;
        }
        
        @Override
        public T getValue() {
            return value;
        }
        
        @Override
        public E getError() {
            throw new IllegalStateException("Cannot get error from Success");
        }
        
        @Override
        public Optional<T> getValueOptional() {
            return Optional.of(value);
        }
        
        @Override
        public Optional<E> getErrorOptional() {
            return Optional.empty();
        }
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            try {
                return success(mapper.apply(value));
            } catch (Exception e) {
                @SuppressWarnings("unchecked")
                Result<U, E> result = (Result<U, E>) failure(e);
                return result;
            }
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            try {
                return mapper.apply(value);
            } catch (Exception e) {
                @SuppressWarnings("unchecked")
                Result<U, E> result = (Result<U, E>) failure(e);
                return result;
            }
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> errorMapper) {
            return success(value);
        }
        
        @Override
        public Result<T, E> filter(Predicate<T> predicate, E errorOnFalse) {
            return predicate.test(value) ? this : failure(errorOnFalse);
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<T> action) {
            action.accept(value);
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<E> action) {
            return this;
        }
        
        @Override
        public Result<T, E> recover(Function<E, T> recovery) {
            return this;
        }
        
        @Override
        public Result<T, E> recoverWith(Function<E, Result<T, E>> recovery) {
            return this;
        }
        
        @Override
        public <U> U match(Function<T, U> onSuccess, Function<E, U> onFailure) {
            return onSuccess.apply(value);
        }
        
        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
        }
    }
    
    /**
     * Failure case implementation
     */
    record Failure<T, E>(E error) implements Result<T, E> {
        @Override
        public boolean isSuccess() {
            return false;
        }
        
        @Override
        public T getValue() {
            throw new IllegalStateException("Cannot get value from Failure");
        }
        
        @Override
        public E getError() {
            return error;
        }
        
        @Override
        public Optional<T> getValueOptional() {
            return Optional.empty();
        }
        
        @Override
        public Optional<E> getErrorOptional() {
            return Optional.of(error);
        }
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return failure(error);
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return failure(error);
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> errorMapper) {
            try {
                return failure(errorMapper.apply(error));
            } catch (Exception e) {
                @SuppressWarnings("unchecked")
                Result<T, F> result = (Result<T, F>) failure(e);
                return result;
            }
        }
        
        @Override
        public Result<T, E> filter(Predicate<T> predicate, E errorOnFalse) {
            return this;
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<T> action) {
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<E> action) {
            action.accept(error);
            return this;
        }
        
        @Override
        public Result<T, E> recover(Function<E, T> recovery) {
            try {
                return success(recovery.apply(error));
            } catch (Exception e) {
                @SuppressWarnings("unchecked")
                Result<T, E> result = (Result<T, E>) failure(e);
                return result;
            }
        }
        
        @Override
        public Result<T, E> recoverWith(Function<E, Result<T, E>> recovery) {
            try {
                return recovery.apply(error);
            } catch (Exception e) {
                @SuppressWarnings("unchecked")
                Result<T, E> result = (Result<T, E>) failure(e);
                return result;
            }
        }
        
        @Override
        public <U> U match(Function<T, U> onSuccess, Function<E, U> onFailure) {
            return onFailure.apply(error);
        }
        
        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }
    }
    
    // Utility methods for common operations
    
    /**
     * Combine two Results into one with a binary function
     */
    static <T, U, V, E> Result<V, E> combine(Result<T, E> first, Result<U, E> second, 
                                           java.util.function.BiFunction<T, U, V> combiner) {
        return first.flatMap(t -> second.map(u -> combiner.apply(t, u)));
    }
    
    /**
     * Sequence a list of Results into a Result of list
     */
    static <T, E> Result<java.util.List<T>, E> sequence(java.util.List<Result<T, E>> results) {
        java.util.List<T> values = new java.util.ArrayList<>();
        for (Result<T, E> result : results) {
            if (result.isFailure()) {
                @SuppressWarnings("unchecked")
                Result<java.util.List<T>, E> failureResult = (Result<java.util.List<T>, E>) result.mapError(Function.identity());
                return failureResult;
            }
            values.add(result.getValue());
        }
        return success(values);
    }
}