package com.apexagro.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.apexagro.TestcontainersConfiguration;

/** BE3-11 DoD: тест превышения лимита (Redis). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RateLimiterTest {

    @Autowired
    RateLimiter rateLimiter;

    @Test
    void allowsUpToLimitThenRejectsWithRetryAfter() {
        String key = "test:" + UUID.randomUUID();
        assertThat(rateLimiter.check(key, 3, Duration.ofMinutes(1)).remaining()).isEqualTo(2);
        assertThat(rateLimiter.check(key, 3, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(rateLimiter.check(key, 3, Duration.ofMinutes(1)).allowed()).isTrue();

        RateLimitResult rejected = rateLimiter.check(key, 3, Duration.ofMinutes(1));
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void windowRefills() throws InterruptedException {
        String key = "test:" + UUID.randomUUID();
        assertThat(rateLimiter.check(key, 1, Duration.ofSeconds(1)).allowed()).isTrue();
        assertThat(rateLimiter.check(key, 1, Duration.ofSeconds(1)).allowed()).isFalse();
        Thread.sleep(1200);
        assertThat(rateLimiter.check(key, 1, Duration.ofSeconds(1)).allowed()).isTrue();
    }

    @Test
    void namedPolicyFromConfiguration() {
        String phone = "+996700" + (100000 + (int) (Math.random() * 899999));
        for (int i = 0; i < 3; i++) {
            rateLimiter.checkOrThrow("otp-send", phone);   // otp-send: 3 за 10 минут
        }
        assertThatThrownBy(() -> rateLimiter.checkOrThrow("otp-send", phone))
                .isInstanceOf(RateLimitExceededException.class);
        assertThatThrownBy(() -> rateLimiter.check("no-such-policy", phone))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
