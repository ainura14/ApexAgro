package com.apexagro.common.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.metadata.ConstraintDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * BE3-04: любая ошибка API — в формате RFC 7807 (см. {@link ProblemDetailFactory}).
 *
 * <p>Модуль может добавить свой {@code @RestControllerAdvice} с более высоким приоритетом,
 * но обычно достаточно бросить {@link ApiException} с кодом из своего enum {@link ErrorCode}.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ProblemDetailFactory problems;

    public ApiExceptionHandler(ProblemDetailFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        ProblemDetail problem = problems.create(ex.errorCode(), ex.messageArgs());
        ex.properties().forEach(problem::setProperty);
        HttpHeaders headers = new HttpHeaders();
        ex.headers().forEach(headers::set);
        logByStatus(ex.errorCode().status(), ex);
        return ResponseEntity.status(problem.getStatus()).headers(headers).body(problem);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.info("Конкурентное изменение: {}", ex.getMessage());
        return respond(problems.create(CommonErrorCode.CONCURRENT_MODIFICATION));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        return respond(problems.create(CommonErrorCode.UNAUTHORIZED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return respond(problems.create(CommonErrorCode.FORBIDDEN));
    }

    /** Валидация на уровне сервисов ({@code @Validated} + ограничения на параметрах методов). */
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldViolation> violations = new ArrayList<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            violations.add(new FieldViolation(lastNode(v.getPropertyPath().toString()),
                    constraintName(v.getConstraintDescriptor()), v.getMessage()));
        }
        return respond(problems.validation(violations));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Необработанная ошибка", ex);
        return respond(problems.create(CommonErrorCode.INTERNAL_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            violations.add(new FieldViolation(error.getField(), error.getCode(), problems.message(error)));
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            violations.add(new FieldViolation(error.getObjectName(), error.getCode(), problems.message(error)));
        }
        return handleExceptionInternal(ex, problems.validation(violations), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError fe ? fe.getField() : parameter;
                violations.add(new FieldViolation(field, lastCode(error), problems.message(error)));
            }
        });
        return handleExceptionInternal(ex, problems.validation(violations), headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Все остальные исключения Spring MVC (400 при нечитаемом JSON, 404, 405, 415 и т. д.)
     * приводятся к нашему формату: код из {@link CommonErrorCode}, текст на языке запроса.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = isOurs(body) ? (ProblemDetail) body : problems.create(codeFor(status), status);
        logByStatus(status, ex);
        return super.handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static boolean isOurs(Object body) {
        if (body instanceof ProblemDetail problem) {
            Map<String, Object> properties = problem.getProperties();
            return properties != null && properties.containsKey(ProblemDetailFactory.CODE);
        }
        return false;
    }

    static CommonErrorCode codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> CommonErrorCode.MALFORMED_REQUEST;
            case 401 -> CommonErrorCode.UNAUTHORIZED;
            case 403 -> CommonErrorCode.FORBIDDEN;
            case 404 -> CommonErrorCode.NOT_FOUND;
            case 405 -> CommonErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> CommonErrorCode.NOT_ACCEPTABLE;
            case 409 -> CommonErrorCode.CONFLICT;
            case 413 -> CommonErrorCode.PAYLOAD_TOO_LARGE;
            case 415 -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 429 -> CommonErrorCode.RATE_LIMITED;
            case 503 -> CommonErrorCode.SERVICE_UNAVAILABLE;
            default -> status.is4xxClientError() ? CommonErrorCode.MALFORMED_REQUEST : CommonErrorCode.INTERNAL_ERROR;
        };
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private static void logByStatus(HttpStatusCode status, Exception ex) {
        if (status.is5xxServerError()) {
            log.error("Ошибка {}: {}", status.value(), ex.getMessage(), ex);
        } else if (log.isDebugEnabled()) {
            log.debug("Ошибка {}: {}", status.value(), ex.getMessage());
        }
    }

    private static String lastCode(MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        return codes == null || codes.length == 0 ? null : codes[codes.length - 1];
    }

    private static String constraintName(ConstraintDescriptor<?> descriptor) {
        return descriptor == null ? null : descriptor.getAnnotation().annotationType().getSimpleName();
    }

    private static String lastNode(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }
}
