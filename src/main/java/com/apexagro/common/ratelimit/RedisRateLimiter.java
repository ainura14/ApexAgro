package com.apexagro.common.ratelimit;

import java.time.Duration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * BE3-11: реализация на Bucket4j + Redis (Lettuce). Окно — «корзина» на {@code limit} токенов,
 * которая целиком пополняется раз в {@code window}. Ключи в Redis: {@code rl:<ключ>:<limit>:<окно>},
 * живут не дольше окна.
 */
public class RedisRateLimiter implements RateLimiter {

    private final ProxyManager<String> buckets;
    private final RateLimitProperties properties;

    public RedisRateLimiter(StatefulRedisConnection<String, byte[]> connection, RateLimitProperties properties) {
        this.buckets = Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
                .build();
        this.properties = properties;
    }

    @Override
    public RateLimitResult check(String key, int limit, Duration window) {
        RateLimitPolicy policy = new RateLimitPolicy(limit, window);
        String redisKey = "rl:" + key + ":" + limit + ":" + window.toSeconds();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(policy.limit()).refillIntervally(policy.limit(), policy.window()).build())
                .build();
        ConsumptionProbe probe = buckets.getProxy(redisKey, () -> configuration).tryConsumeAndReturnRemaining(1);
        return probe.isConsumed()
                ? new RateLimitResult(true, probe.getRemainingTokens(), Duration.ZERO)
                : new RateLimitResult(false, 0, Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    @Override
    public RateLimitResult check(String policyName, String subject) {
        RateLimitPolicy policy = properties.policy(policyName);
        return check(policyName + ":" + subject, policy.limit(), policy.window());
    }
}
