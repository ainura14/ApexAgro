package com.apexagro.common.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apexagro.common.logging.CorrelationId;

/**
 * BE3-09: базовый класс идемпотентного обработчика событий и команд.
 *
 * <pre>
 * &#64;Component
 * class ShipmentTrackingOnOrderPlaced extends IdempotentHandler {
 *
 *     ShipmentTrackingOnOrderPlaced(ProcessedEvents processed) {
 *         super(processed, "delivery", "delivery.tracking-on-order-placed");
 *     }
 *
 *     &#64;ApplicationModuleListener                 // своя транзакция, асинхронно, с записью в event_publication
 *     void on(OrderPlaced event) {
 *         handleOnce(event, () -&gt; createTracking(event));
 *     }
 * }
 * </pre>
 * Повторная доставка того же события (republish после рестарта, ретрай RabbitMQ) не выполнит действие второй раз.
 * Метод обработчика обязан быть транзакционным ({@code @ApplicationModuleListener}, {@code @Transactional}).
 */
public abstract class IdempotentHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ProcessedEvents processedEvents;
    private final String schema;
    private final String consumer;

    protected IdempotentHandler(ProcessedEvents processedEvents, String schema, String consumer) {
        this.processedEvents = processedEvents;
        this.schema = schema;
        this.consumer = consumer;
    }

    protected final void handleOnce(DomainEvent event, Runnable action) {
        EventMetadata metadata = event.metadata();
        CorrelationId.runWith(metadata.correlationId(), () -> handleOnce(metadata.eventId(), action));
    }

    protected final void handleOnce(UUID eventId, Runnable action) {
        if (processedEvents.markProcessed(schema, eventId, consumer)) {
            action.run();
        } else {
            log.info("Событие {} уже обработано потребителем {}, пропуск", eventId, consumer);
        }
    }

    protected final String consumer() {
        return consumer;
    }
}
