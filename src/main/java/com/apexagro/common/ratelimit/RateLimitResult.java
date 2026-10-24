package com.apexagro.common.ratelimit;

import java.time.Duration;

/**
 * BE3-11: результат проверки лимита.
 *
 * @param allowed    запрос разрешён (и учтён)
 * @param remaining  сколько ещё запросов осталось в окне
 * @param retryAfter через сколько повторить, если запрещён ({@link Duration#ZERO}, если разрешён)
 */
public record RateLimitResult(boolean allowed, long remaining, Duration retryAfter) {
}
