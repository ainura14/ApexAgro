package com.apexagro.common.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * BE3-04: бизнес-ошибка с кодом из реестра. Превращается в ответ RFC 7807 в {@link ApiExceptionHandler}.
 *
 * <pre>
 * throw new ApiException(DeliveryErrorCode.INVALID_STATUS_TRANSITION, from, to)   // аргументы → {0}, {1} в тексте
 *         .with("allowed", allowedStatuses);                                     // доп. поле в ответе
 * </pre>
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] messageArgs;
    private final transient Map<String, Object> properties = new LinkedHashMap<>();
    private final transient Map<String, String> headers = new LinkedHashMap<>();

    public ApiException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.code());
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs.clone();
    }

    public ApiException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        this(errorCode, messageArgs);
        initCause(cause);
    }

    /** Дополнительное поле в теле ответа (например, {@code resetsAt} для лимитов). */
    public ApiException with(String property, Object value) {
        properties.put(property, value);
        return this;
    }

    /** Дополнительный HTTP-заголовок ответа (например, {@code Retry-After}). */
    public ApiException withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Object[] messageArgs() {
        return messageArgs.clone();
    }

    public Map<String, Object> properties() {
        return Collections.unmodifiableMap(properties);
    }
}
