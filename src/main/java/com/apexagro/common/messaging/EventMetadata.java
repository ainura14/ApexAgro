package com.apexagro.common.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.apexagro.common.logging.CorrelationId;

/**
 * BE3-09: служебные поля каждого события и команды (EVENTS.md).
 *
 * <pre>
 * public record OrderPlaced(EventMetadata metadata, UUID orderId, UUID buyerId, ...) implements DomainEvent { }
 * events.publishEvent(new OrderPlaced(EventMetadata.create(), order.id(), ...));
 * </pre>
 *
 * @param eventId       уникальный id; ключ идемпотентности в {@code <schema>.processed_events}
 * @param occurredAt    время события (UTC)
 * @param correlationId id запроса, породившего событие; обработчик восстанавливает его в логах
 */
public record EventMetadata(UUID eventId, Instant occurredAt, String correlationId) {

    public EventMetadata {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static EventMetadata create() {
        return new EventMetadata(UUID.randomUUID(), Instant.now(), CorrelationId.current().orElse(null));
    }
}
