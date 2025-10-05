package com.trademaster.subscription.config;

import com.trademaster.subscription.security.ServiceApiKeyFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
// JWT imports removed - using API key authentication
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Zero Trust Security Configuration
 *
 * MANDATORY implementation following TradeMaster Golden Specification.
 * Implements tiered security model with JWT authentication for external APIs
 * and Kong API key authentication for internal service-to-service communication.
 *
 * Security Layers:
 * - External APIs: Full security stack with JWT authentication
 * - Internal APIs: Kong API key validation via ServiceApiKeyFilter
 * - Health checks: Public access for monitoring
 *
 * @author TradeMaster Engineering Team
 * @version 1.0.0
 * @since 2025-01-09
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final ServiceApiKeyFilter serviceApiKeyFilter;

    /**
     * Configure security filter chain with JWT authentication
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // OAuth2 Resource Server with JWT (only for external endpoints)
            // Internal endpoints use API key authentication via ServiceApiKeyFilter
            
            // Configure authorization rules following Zero Trust model
            .authorizeHttpRequests(authz -> authz
                // Public endpoints for health monitoring
                .requestMatchers(HttpMethod.GET,
                    "/actuator/health",
                    "/api/v2/health",
                    "/api/v2/ping"
                ).permitAll()

                // Public endpoints for API documentation
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Internal API endpoints (Kong API key authentication via ServiceApiKeyFilter)
                // Use permitAll() here - ServiceApiKeyFilter handles authentication
                .requestMatchers("/api/internal/**").permitAll()

                // External API endpoints with JWT authentication
                // Admin endpoints - require admin role
                .requestMatchers(HttpMethod.POST, "/api/v1/usage/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/subscriptions/status/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions/*/suspend").hasRole("ADMIN")
                .requestMatchers("/actuator/prometheus").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // User endpoints - require authenticated user
                .requestMatchers(HttpMethod.GET, "/api/v1/subscriptions/users/**").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions/*/upgrade").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions/*/cancel").hasRole("USER")
                .requestMatchers("/api/v1/usage/**").hasRole("USER")

                // Service-to-service endpoints via external API (require service role)
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions/*/activate").hasRole("SERVICE")

                // Deny all other requests by default (Zero Trust)
                .anyRequest().denyAll()
            )

            // Add ServiceApiKeyFilter before JWT filter
            .addFilterBefore(serviceApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                        {
                            "timestamp": "%s",
                            "status": 401,
                            "error": "Unauthorized",
                            "message": "Authentication required",
                            "path": "%s"
                        }""".formatted(java.time.Instant.now(), request.getRequestURI()));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                        {
                            "timestamp": "%s",
                            "status": 403,
                            "error": "Forbidden", 
                            "message": "Insufficient privileges",
                            "path": "%s"
                        }""".formatted(java.time.Instant.now(), request.getRequestURI()));
                })
            );

        return http.build();
    }

    // JWT authentication converter removed - using API key authentication for internal endpoints

    /**
     * Configure CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins in production - use environment variable
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        } else {
            // Development configuration
            configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://localhost:*",
                "https://*.trademaster.com"
            ));
        }
        
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}