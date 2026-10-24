/**
 * Модуль notification (BE3): центр уведомлений, Web Push, SMS.
 *
 * <p>Шаблон модуля для всей команды:
 * <pre>
 * com.apexagro.notification            — публичный API: SmsApi, DTO, события модуля
 * com.apexagro.notification.internal   — реализация: сервисы, репозитории, сущности, контроллеры
 * </pre>
 * Другие модули видят только корневой пакет. allowedDependencies — явный список модулей,
 * от которых можно зависеть; меняется только через ревью тимлида.
 */
@ApplicationModule(displayName = "Notification", allowedDependencies = "common")
package com.apexagro.notification;

import org.springframework.modulith.ApplicationModule;
