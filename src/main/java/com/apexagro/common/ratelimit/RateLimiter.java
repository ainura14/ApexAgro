package com.apexagro.common.ratelimit;

import java.time.Duration;

/**
 * BE3-11: ограничение частоты запросов, общее для всех экземпляров приложения (Redis).
 *
 * <pre>
 * // лимит из application.yml (apexagro.rate-limits.policies.otp-send)
 * rateLimiter.checkOrThrow("otp-send", phone);
 *
 * // или явно
 * RateLimitResult r = rateLimiter.check("upload:" + userId, 30, Duration.ofMinutes(1));
 * </pre>
 * При превышении {@link #checkOrThrow} бросает {@link RateLimitExceededException} → 429 RATE_LIMITED
 * с заголовком {@code Retry-After}.
 */
public interface RateLimiter {

    RateLimitResult check(String key, int limit, Duration window);

    /** Лимит по имени политики из конфигурации; ключ = имя политики + субъект (телефон, userId, IP). */
    RateLimitResult check(String policyName, String subject);

    default void checkOrThrow(String policyName, String subject) {
        RateLimitResult result = check(policyName, subject);
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfter());
        }
    }
}
