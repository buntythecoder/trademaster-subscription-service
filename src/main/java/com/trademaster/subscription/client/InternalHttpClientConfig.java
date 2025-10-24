package com.trademaster.subscription.client;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Internal HTTP Client Configuration
 * MANDATORY: Single Responsibility - HTTP client creation only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Factory for creating OkHttpClient with Virtual Threads optimization.
 * All timeouts externalized per Rule #16 (Dynamic Configuration).
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class InternalHttpClientConfig {

    @Value("${trademaster.internal-service.timeout.connect-seconds:5}")
    private int connectTimeoutSeconds;

    @Value("${trademaster.internal-service.timeout.write-seconds:10}")
    private int writeTimeoutSeconds;

    @Value("${trademaster.internal-service.timeout.read-seconds:30}")
    private int readTimeoutSeconds;

    /**
     * Create HTTP client optimized for Virtual Threads
     * MANDATORY: All timeouts externalized per Rule #16
     */
    @Bean(name = "internalServiceHttpClient")
    public OkHttpClient createHttpClient() {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

        log.info("Internal service HTTP client initialized with Virtual Threads support " +
                "(connect: {}s, write: {}s, read: {}s)",
                connectTimeoutSeconds, writeTimeoutSeconds, readTimeoutSeconds);

        return client;
    }
}
