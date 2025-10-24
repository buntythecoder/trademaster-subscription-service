package com.trademaster.subscription.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Service Authentication Setter
 * MANDATORY: Single Responsibility - Spring Security context management only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Sets Spring Security authentication context for internal service calls.
 * Grants ROLE_SERVICE and ROLE_INTERNAL authorities.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class ServiceAuthenticationSetter {

    private static final List<SimpleGrantedAuthority> SERVICE_AUTHORITIES = List.of(
        new SimpleGrantedAuthority("ROLE_SERVICE"),
        new SimpleGrantedAuthority("ROLE_INTERNAL")
    );

    /**
     * Set Spring Security authentication context for service calls
     *
     * @param serviceId Calling service identifier
     */
    public void setServiceAuthentication(String serviceId) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(serviceId, null, SERVICE_AUTHORITIES);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set SERVICE authentication for: {} with authorities: {}", serviceId, SERVICE_AUTHORITIES);
    }
}
