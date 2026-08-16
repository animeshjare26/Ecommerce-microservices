package com.ecommerce.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * GatewayConfig (The Bouncer's Clicker)
 * 
 * This file configures the Rate Limiter for the API Gateway.
 * 
 * WHY WE ADDED IT:
 * To prevent hackers from brute-forcing passwords or spamming our API, we added Rate Limiting.
 * However, the gateway needs a way to identify WHO is making the requests so it can limit them individually.
 * Since the Gateway doesn't look inside JWTs (it's a "dumb router"), this `ipKeyResolver` acts like a 
 * bouncer at a club clicking a counter based on the visitor's IP Address.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        // Rate limits based on the client's IP address
        return exchange -> Mono.just(Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress());
    }
}
