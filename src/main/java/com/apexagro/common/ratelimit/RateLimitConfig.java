package com.apexagro.common.ratelimit;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
class RateLimitConfig {

    /** Отдельное соединение поверх клиента Boot: тот же хост, пароль и SSL, что у spring.data.redis. */
    @Bean(destroyMethod = "close")
    @Lazy
    StatefulRedisConnection<String, byte[]> rateLimiterRedisConnection(LettuceConnectionFactory factory) {
        RedisClient client = (RedisClient) factory.getRequiredNativeClient();
        return client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    RateLimiter rateLimiter(@Lazy StatefulRedisConnection<String, byte[]> rateLimiterRedisConnection,
            RateLimitProperties properties) {
        return new LazyRateLimiter(() -> new RedisRateLimiter(rateLimiterRedisConnection, properties));
    }
}
