package com.trademaster.subscription.common;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Success Case Implementation for Result Pattern
 * MANDATORY: Single Responsibility - Success case handling only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Represents successful result in Railway Programming pattern.
 *
 * @author TradeMaster Development Team
 */
public record ResultSuccess<T, E>(T value) implements Result<T, E> {

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
            return Result.success(mapper.apply(value));
        } catch (Exception e) {
            @SuppressWarnings("unchecked")
            Result<U, E> result = (Result<U, E>) Result.failure(e);
            return result;
        }
    }

    @Override
    public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        try {
            return mapper.apply(value);
        } catch (Exception e) {
            @SuppressWarnings("unchecked")
            Result<U, E> result = (Result<U, E>) Result.failure(e);
            return result;
        }
    }

    @Override
    public <F> Result<T, F> mapError(Function<E, F> errorMapper) {
        return Result.success(value);
    }

    @Override
    public Result<T, E> filter(Predicate<T> predicate, E errorOnFalse) {
        return predicate.test(value) ? this : Result.failure(errorOnFalse);
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
