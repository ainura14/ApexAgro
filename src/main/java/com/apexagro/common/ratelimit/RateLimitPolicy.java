package com.apexagro.common.ratelimit;

import java.time.Duration;

/** BE3-11: лимит — {@code limit} запросов за {@code window}. */
public record RateLimitPolicy(int limit, Duration window) {

    public RateLimitPolicy {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit должен быть > 0");
        }
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("window должен быть > 0");
        }
    }
}
