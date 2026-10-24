/**
 * Общий каркас (BE3): ошибки RFC 7807, i18n, конвенции API, логи и correlation id,
 * события и RabbitMQ, rate limiter, планировщик, безопасность инфраструктурных эндпоинтов.
 *
 * <p>OPEN-модуль: его подпакеты ({@code common.api}, {@code common.error} и т. д.) доступны всем модулям.
 * Сам {@code common} не зависит от бизнес-модулей (проверяет ArchitectureRulesTests).
 * Изменения общих конвенций — только через ревью тимлида (CODEOWNERS).
 */
@ApplicationModule(displayName = "Common", type = ApplicationModule.Type.OPEN)
package com.apexagro.common;

import org.springframework.modulith.ApplicationModule;
