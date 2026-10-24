package com.apexagro.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.events.Externalized;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.apexagro.TestcontainersConfiguration;
import com.apexagro.common.logging.CorrelationId;

/**
 * BE3-09 DoD: пример события и команды проходит end-to-end; повторная доставка не выполняет действие дважды;
 * команда, которая падает, после 3 попыток оказывается в DLQ.
 *
 * Событие: publishEvent в транзакции → common.event_publication → @ApplicationModuleListener.
 * Команда: publishEvent в транзакции → внешняя публикация (@Externalized) в apexagro.commands → очередь → обработчик.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MessagingIntegrationTest.Handlers.class})
class MessagingIntegrationTest {

    static final String SCHEMA = "messaging_test";
    static final String OK_COMMAND = "test.sample-ok";
    static final String FAILING_COMMAND = "test.sample-fail";

    record SampleEvent(EventMetadata metadata, String payload) implements DomainEvent {
    }

    @Externalized(Messaging.COMMANDS_EXCHANGE + "::" + OK_COMMAND)
    record SampleCommand(EventMetadata metadata, String payload) implements DomainEvent {
    }

    @Externalized(Messaging.COMMANDS_EXCHANGE + "::" + FAILING_COMMAND)
    record FailingCommand(EventMetadata metadata) implements DomainEvent {
    }

    static final List<String> handledEvents = new CopyOnWriteArrayList<>();
    static final List<String> handledCommands = new CopyOnWriteArrayList<>();
    static final List<String> seenCorrelationIds = new CopyOnWriteArrayList<>();
    static final AtomicInteger failingAttempts = new AtomicInteger();

    @TestConfiguration
    static class Handlers {

        @Bean
        Declarables sampleQueues() {
            Declarables ok = Messaging.commandQueue(OK_COMMAND);
            Declarables failing = Messaging.commandQueue(FAILING_COMMAND);
            List<org.springframework.amqp.core.Declarable> all = new java.util.ArrayList<>(ok.getDeclarables());
            all.addAll(failing.getDeclarables());
            return new Declarables(all);
        }

        @Bean
        SampleEventHandler sampleEventHandler(ProcessedEvents processed) {
            return new SampleEventHandler(processed);
        }

        @Bean
        SampleCommandHandler sampleCommandHandler(ProcessedEvents processed) {
            return new SampleCommandHandler(processed);
        }
    }

    static class SampleEventHandler extends IdempotentHandler {

        SampleEventHandler(ProcessedEvents processed) {
            super(processed, SCHEMA, "test.sample-event-handler");
        }

        @ApplicationModuleListener
        void on(SampleEvent event) {
            handleOnce(event, () -> {
                handledEvents.add(event.payload());
                seenCorrelationIds.add(MDC.get(CorrelationId.MDC_KEY));
            });
        }
    }

    static class SampleCommandHandler extends IdempotentHandler {

        SampleCommandHandler(ProcessedEvents processed) {
            super(processed, SCHEMA, "test.sample-command-handler");
        }

        @RabbitListener(queues = Messaging.COMMAND_QUEUE_PREFIX + OK_COMMAND)
        @Transactional
        void handle(SampleCommand command) {
            handleOnce(command, () -> {
                handledCommands.add(command.payload());
                seenCorrelationIds.add(MDC.get(CorrelationId.MDC_KEY));
            });
        }

        @RabbitListener(queues = Messaging.COMMAND_QUEUE_PREFIX + FAILING_COMMAND)
        void handleFailing(FailingCommand command) {
            failingAttempts.incrementAndGet();
            throw new IllegalStateException("обработчик всегда падает");
        }
    }

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    RabbitTemplate rabbit;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA);
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + SCHEMA + ".processed_events (event_id uuid NOT NULL, "
                + "consumer varchar(100) NOT NULL, processed_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY (event_id, consumer))");
        handledEvents.clear();
        handledCommands.clear();
        seenCorrelationIds.clear();
        failingAttempts.set(0);
    }

    private void publishInTransaction(Object event, String correlationId) {
        CorrelationId.runWith(correlationId, () -> tx.executeWithoutResult(status -> events.publishEvent(event)));
    }

    @Test
    void eventIsDeliveredThroughPublicationRegistryWithCorrelationId() {
        publishInTransaction(new SampleEvent(metadataWith("corr-event"), "hello"), "corr-event");

        await().atMost(Duration.ofSeconds(10)).until(() -> handledEvents.contains("hello"));
        assertThat(seenCorrelationIds).contains("corr-event");
        await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.queryForObject(
                "SELECT count(*) FROM common.event_publication WHERE completion_date IS NULL", Long.class) == 0);
    }

    @Test
    void commandGoesThroughRabbitMqAndDuplicateIsIgnored() {
        EventMetadata metadata = metadataWith("corr-cmd");
        SampleCommand command = new SampleCommand(metadata, "do-it");

        publishInTransaction(command, "corr-cmd");
        await().atMost(Duration.ofSeconds(15)).until(() -> handledCommands.contains("do-it"));
        assertThat(seenCorrelationIds).contains("corr-cmd");

        // повторная доставка того же сообщения (как при ретрае или republish)
        rabbit.convertAndSend(Messaging.COMMANDS_EXCHANGE, OK_COMMAND, command);
        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).until(() -> true);
        assertThat(handledCommands).containsExactly("do-it");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + SCHEMA + ".processed_events WHERE event_id = ?",
                Long.class, metadata.eventId())).isEqualTo(1);
    }

    @Test
    void failingCommandIsRetriedThreeTimesThenGoesToDlq() {
        publishInTransaction(new FailingCommand(EventMetadata.create()), "corr-fail");

        String dlq = Messaging.deadLetterQueueName(FAILING_COMMAND);
        await().atMost(Duration.ofSeconds(30)).until(() -> rabbit.receive(dlq, 500) != null);
        assertThat(failingAttempts.get()).isEqualTo(3);
    }

    private static EventMetadata metadataWith(String correlationId) {
        return new EventMetadata(UUID.randomUUID(), java.time.Instant.now(), correlationId);
    }
}
