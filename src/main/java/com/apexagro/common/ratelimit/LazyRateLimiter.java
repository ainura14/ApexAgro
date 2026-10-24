package com.apexagro.common.ratelimit;

import java.time.Duration;
import java.util.function.Supplier;

/** Подключение к Redis — при первом вызове, а не на старте: приложение поднимается и без Redis. */
final class LazyRateLimiter implements RateLimiter {

    private final Supplier<RateLimiter> factory;
    private volatile RateLimiter delegate;

    LazyRateLimiter(Supplier<RateLimiter> factory) {
        this.factory = factory;
    }

    private RateLimiter delegate() {
        RateLimiter result = delegate;
        if (result == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = factory.get();
                }
                result = delegate;
            }
        }
        return result;
    }

    @Override
    public RateLimitResult check(String key, int limit, Duration window) {
        return delegate().check(key, limit, window);
    }

    @Override
    public RateLimitResult check(String policyName, String subject) {
        return delegate().check(policyName, subject);
    }
}
