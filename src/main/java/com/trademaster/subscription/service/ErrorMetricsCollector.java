package com.trademaster.subscription.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Error Metrics Collector
 * MANDATORY: Single Responsibility - Prometheus metrics collection only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles all Prometheus metrics collection for error tracking.
 * Separated from ErrorTrackingService to maintain SRP.
 *
 * @author TradeMaster Development Team
 */
@Service
@RequiredArgsConstructor
@Getter
public class ErrorMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final Counter errorCounter;
    private final Counter criticalErrorCounter;
    private final Timer errorProcessingTimer;

    public ErrorMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.errorCounter = Counter.builder("subscription.errors.total")
                .description("Total number of errors tracked")
                .register(meterRegistry);

        this.criticalErrorCounter = Counter.builder("subscription.errors.critical")
                .description("Number of critical errors tracked")
                .register(meterRegistry);

        this.errorProcessingTimer = Timer.builder("subscription.error.processing.duration")
                .description("Time taken to process and track errors")
                .register(meterRegistry);
    }

    /**
     * Increment total error counter
     */
    public void incrementErrorCount() {
        errorCounter.increment();
    }

    /**
     * Increment critical error counter
     */
    public void incrementCriticalErrorCount() {
        criticalErrorCounter.increment();
    }

    /**
     * Start timing error processing
     */
    public Timer.Sample startErrorProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop timing error processing
     */
    public void stopErrorProcessingTimer(Timer.Sample sample) {
        sample.stop(errorProcessingTimer);
    }
}
