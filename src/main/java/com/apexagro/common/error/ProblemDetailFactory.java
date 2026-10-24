package com.apexagro.common.error;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import com.apexagro.common.logging.CorrelationId;

/**
 * BE3-04: собирает тело ошибки в едином формате.
 *
 * <pre>
 * {
 *   "type": "about:blank",
 *   "title": "Conflict",
 *   "status": 409,
 *   "detail": "Нельзя перевести отправление из статуса CREATED в DELIVERED",   ← на языке запроса
 *   "instance": "/api/v1/seller/shipments/…/status",
 *   "code": "INVALID_STATUS_TRANSITION",                                        ← для фронтенда
 *   "traceId": "6f1c…",                                                         ← = X-Correlation-Id, по нему ищут в логах
 *   "errors": [ { "field": "toStatus", "code": "NotNull", "message": "…" } ]   ← только для VALIDATION_FAILED
 * }
 * </pre>
 */
@Component
public class ProblemDetailFactory {

    public static final String CODE = "code";
    public static final String TRACE_ID = "traceId";
    public static final String ERRORS = "errors";

    private final MessageSource messageSource;

    public ProblemDetailFactory(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public ProblemDetail create(ErrorCode code, Object... args) {
        return create(code, code.status(), args);
    }

    public ProblemDetail create(ErrorCode code, HttpStatusCode status, Object... args) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message(code, args));
        HttpStatus known = HttpStatus.resolve(status.value());
        problem.setTitle(known != null ? known.getReasonPhrase() : String.valueOf(status.value()));
        problem.setProperty(CODE, code.code());
        CorrelationId.current().ifPresent(id -> problem.setProperty(TRACE_ID, id));
        return problem;
    }

    public ProblemDetail validation(List<FieldViolation> violations) {
        ProblemDetail problem = create(CommonErrorCode.VALIDATION_FAILED);
        problem.setProperty(ERRORS, List.copyOf(violations));
        return problem;
    }

    public String message(ErrorCode code, Object... args) {
        return messageSource.getMessage(code.messageKey(), args, code.code(), locale());
    }

    public String message(MessageSourceResolvable resolvable) {
        return messageSource.getMessage(resolvable, locale());
    }

    private static Locale locale() {
        return LocaleContextHolder.getLocale();
    }
}
