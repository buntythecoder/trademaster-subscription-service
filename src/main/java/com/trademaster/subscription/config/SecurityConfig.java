package com.trademaster.subscription.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security Configuration
 * 
 * Configures JWT-based authentication and authorization for the subscription service.
 * Implements TradeMaster security standards with proper CORS and endpoint protection.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

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
            
            // Configure OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> 
                oauth2.jwt(jwt -> 
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers(HttpMethod.GET, 
                    "/actuator/health",
                    "/actuator/info",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                
                // Admin endpoints - require admin role
                .requestMatchers(HttpMethod.POST, "/api/v1/usage/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/subscriptions/status/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions/*/suspend").hasRole("ADMIN")
                
                // User endpoints - require authenticated user
                .requestMatchers(HttpMethod.GET, "/api/v1/subscriptions/users/**").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions/*/upgrade").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions/*/cancel").hasRole("USER")
                
                // Usage endpoints - require authenticated user
                .requestMatchers("/api/v1/usage/**").hasRole("USER")
                
                // Service-to-service endpoints - require service role
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions/*/activate").hasRole("SERVICE")
                
                // Metrics endpoint - require admin role  
                .requestMatchers("/actuator/prometheus").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
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

    /**
     * Configure JWT authentication converter
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        authenticationConverter.setPrincipalClaimName("sub");

        return authenticationConverter;
    }

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