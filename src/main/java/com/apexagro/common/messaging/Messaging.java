package com.apexagro.common.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

/**
 * BE3-09: топология RabbitMQ и соглашения об именах.
 *
 * <pre>
 * apexagro.commands  (direct)  ── routing key = имя команды ──▶  apexagro.cmd.&lt;команда&gt;
 *                                                                  │ 3 попытки (ретраи в приложении)
 *                                                                  ▼ отказ
 * apexagro.dlx       (direct)  ── routing key = имя команды ──▶  apexagro.cmd.&lt;команда&gt;.dlq
 * apexagro.realtime  (topic)   — рассылка между экземплярами (чат, live-статусы)
 * </pre>
 *
 * <p>Имя команды: {@code <модуль>.<действие>}, например {@code notification.send-push} (C-01).
 *
 * <p>Объявить очередь своей команды в модуле:
 * <pre>
 * &#64;Bean
 * Declarables sendPushQueue() {
 *     return Messaging.commandQueue(SendPushCommand.NAME);
 * }
 * </pre>
 * Опубликовать команду — как событие Spring Modulith внутри транзакции (outbox), с
 * {@code @Externalized(Messaging.COMMANDS_EXCHANGE + "::" + NAME)} на классе команды.
 */
public final class Messaging {

    public static final String COMMANDS_EXCHANGE = "apexagro.commands";
    public static final String DEAD_LETTER_EXCHANGE = "apexagro.dlx";
    public static final String REALTIME_EXCHANGE = "apexagro.realtime";

    public static final String COMMAND_QUEUE_PREFIX = "apexagro.cmd.";
    public static final String DLQ_SUFFIX = ".dlq";

    private Messaging() {
    }

    public static String queueName(String commandName) {
        return COMMAND_QUEUE_PREFIX + commandName;
    }

    public static String deadLetterQueueName(String commandName) {
        return queueName(commandName) + DLQ_SUFFIX;
    }

    /** Очередь команды + её DLQ + привязки к {@code apexagro.commands} и {@code apexagro.dlx}. */
    public static Declarables commandQueue(String commandName) {
        if (commandName == null || !commandName.matches("[a-z][a-z0-9-]*\\.[a-z][a-z0-9.-]*")) {
            throw new IllegalArgumentException("Имя команды должно быть вида <модуль>.<действие>: " + commandName);
        }
        Queue queue = QueueBuilder.durable(queueName(commandName))
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(commandName)
                .build();
        Queue deadLetters = QueueBuilder.durable(deadLetterQueueName(commandName)).build();
        Binding toQueue = BindingBuilder.bind(queue).to(new DirectExchange(COMMANDS_EXCHANGE)).with(commandName);
        Binding toDlq = BindingBuilder.bind(deadLetters).to(new DirectExchange(DEAD_LETTER_EXCHANGE)).with(commandName);
        return new Declarables(queue, deadLetters, toQueue, toDlq);
    }
}
