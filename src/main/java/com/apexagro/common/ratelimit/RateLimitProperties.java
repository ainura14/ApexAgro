package com.apexagro.common.ratelimit;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** BE3-11: {@code apexagro.rate-limits.policies.<имя>.limit/window} в application.yml. */
@ConfigurationProperties("apexagro.rate-limits")
public record RateLimitProperties(Map<String, RateLimitPolicy> policies) {

    public RateLimitProperties {
        policies = policies == null ? Map.of() : Map.copyOf(policies);
    }

    public RateLimitPolicy policy(String name) {
        RateLimitPolicy policy = policies.get(name);
        if (policy == null) {
            throw new IllegalArgumentException("Лимит '" + name + "' не описан в apexagro.rate-limits.policies");
        }
        return policy;
    }
}
