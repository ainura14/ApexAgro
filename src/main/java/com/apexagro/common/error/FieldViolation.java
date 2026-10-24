package com.apexagro.common.error;

/**
 * BE3-04: элемент {@code errors[]} в ответе 400 VALIDATION_FAILED.
 *
 * @param field   путь поля в запросе ({@code items[0].quantity}); для ошибок объекта целиком — имя объекта
 * @param code    имя нарушенного правила ({@code NotBlank}, {@code Size}, {@code Min})
 * @param message текст на языке запроса
 */
public record FieldViolation(String field, String code, String message) {
}
