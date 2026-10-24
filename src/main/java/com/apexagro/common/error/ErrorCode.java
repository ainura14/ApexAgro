package com.apexagro.common.error;

import org.springframework.http.HttpStatus;

/**
 * BE3-04: реестр кодов ошибок API. Код — стабильная строка для фронтенда ({@code INVALID_STATUS_TRANSITION}),
 * HTTP-статус — по смыслу, текст — из бандлов {@code messages_ru/ky.properties} по ключу {@code error.<CODE>}.
 *
 * <p>Каждый модуль заводит свой enum, реализующий этот интерфейс, в публичном пакете модуля, например:
 * <pre>
 * public enum DeliveryErrorCode implements ErrorCode {
 *     INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT),
 *     EXPECTED_DATE_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT);
 *     ...
 * }
 * </pre>
 * ErrorCodeRegistryTest проверяет, что коды уникальны во всём приложении и у каждого есть тексты на ky и ru.
 */
public interface ErrorCode {

    /** Машинный код, UPPER_SNAKE_CASE. Для enum совпадает с именем константы. */
    String code();

    HttpStatus status();

    /** Ключ текста в бандле сообщений. */
    default String messageKey() {
        return "error." + code();
    }
}
