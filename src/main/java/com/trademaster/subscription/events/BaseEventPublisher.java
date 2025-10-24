package com.trademaster.subscription.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.subscription.common.Result;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Base Event Publisher
 * MANDATORY: Single Responsibility - Shared Kafka infrastructure only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Provides common Kafka publishing infrastructure with circuit breaker resilience.
 * All specialized publishers extend this class to eliminate code duplication.
 *
 * @author TradeMaster Development Team
 */
@RequiredArgsConstructor
@Slf4j
public abstract class BaseEventPublisher {

    protected final KafkaTemplate<String, String> kafkaTemplate;
    protected final ObjectMapper objectMapper;
    protected final CircuitBreaker kafkaCircuitBreaker;

    /**
     * Generic event publishing with circuit breaker and virtual threads
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    protected <T> CompletableFuture<Result<Void, String>> publishToKafka(
            String topic, String key, T event, String eventContext) {

        return CompletableFuture.supplyAsync(() ->
            executeWithResilience(() -> serializeAndPublish(topic, key, event, eventContext)),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Serialize and publish event to Kafka
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    private <T> Result<Void, String> serializeAndPublish(String topic, String key, T event, String context) {
        return serializeEvent(event, context)
            .flatMap(eventJson -> sendToKafka(topic, key, eventJson, context));
    }

    /**
     * Serialize event to JSON
     * MANDATORY: Functional Programming - Rule #3 (NO if-else)
     */
    private <T> Result<String, String> serializeEvent(T event, String context) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            return Result.success(eventJson);
        } catch (JsonProcessingException e) {
            log.error("{} serialization failed", context, e);
            return Result.failure(context + " serialization failed: " + e.getMessage());
        }
    }

    /**
     * Send serialized event to Kafka topic
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    private Result<Void, String> sendToKafka(String topic, String key, String eventJson, String context) {
        try {
            log.info("Publishing {} event", context);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, eventJson);
            future.join(); // Wait for completion
            return Result.success(null);
        } catch (Exception e) {
            log.error("{} publishing failed", context, e);
            return Result.failure(context + " publishing failed: " + e.getMessage());
        }
    }

    /**
     * Execute operation with circuit breaker resilience
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    protected <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return kafkaCircuitBreaker.executeSupplier(operation);
    }
}
