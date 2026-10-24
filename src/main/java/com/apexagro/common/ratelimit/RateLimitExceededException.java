package com.apexagro.common.ratelimit;

import java.time.Duration;

import org.springframework.http.HttpHeaders;

import com.apexagro.common.error.ApiException;
import com.apexagro.common.error.CommonErrorCode;

/** BE3-11: 429 RATE_LIMITED, {@code Retry-After} в секундах и поле {@code retryAfterSeconds} в теле. */
public class RateLimitExceededException extends ApiException {

    public RateLimitExceededException(Duration retryAfter) {
        super(CommonErrorCode.RATE_LIMITED);
        long seconds = Math.max(1, (long) Math.ceil(retryAfter.toMillis() / 1000.0));
        withHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
        with("retryAfterSeconds", seconds);
    }
}
