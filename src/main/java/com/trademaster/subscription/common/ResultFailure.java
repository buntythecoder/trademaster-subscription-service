package com.trademaster.subscription.common;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Failure Case Implementation for Result Pattern
 * MANDATORY: Single Responsibility - Failure case handling only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Represents failure result in Railway Programming pattern.
 *
 * @author TradeMaster Development Team
 */
public record ResultFailure<T, E>(E error) implements Result<T, E> {

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
        return Result.failure(error);
    }

    @Override
    public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return Result.failure(error);
    }

    @Override
    public <F> Result<T, F> mapError(Function<E, F> errorMapper) {
        try {
            return Result.failure(errorMapper.apply(error));
        } catch (Exception e) {
            @SuppressWarnings("unchecked")
            Result<T, F> result = (Result<T, F>) Result.failure(e);
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
            return Result.success(recovery.apply(error));
        } catch (Exception e) {
            @SuppressWarnings("unchecked")
            Result<T, E> result = (Result<T, E>) Result.failure(e);
            return result;
        }
    }

    @Override
    public Result<T, E> recoverWith(Function<E, Result<T, E>> recovery) {
        try {
            return recovery.apply(error);
        } catch (Exception e) {
            @SuppressWarnings("unchecked")
            Result<T, E> result = (Result<T, E>) Result.failure(e);
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
