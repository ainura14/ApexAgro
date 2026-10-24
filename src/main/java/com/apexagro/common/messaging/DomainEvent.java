package com.apexagro.common.messaging;

/**
 * BE3-09: маркер событий и команд приложения. Даёт обработчикам единый доступ к id (идемпотентность)
 * и correlation id (логи).
 */
public interface DomainEvent {

    EventMetadata metadata();
}
