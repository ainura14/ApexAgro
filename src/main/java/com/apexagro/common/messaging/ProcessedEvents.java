package com.apexagro.common.messaging;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * BE3-09: журнал обработанных событий модуля ({@code <schema>.processed_events}, PK {@code (event_id, consumer)}).
 * Вызывается в транзакции обработчика: если обработка упадёт, отметка откатится вместе с ней и повтор пройдёт заново.
 */
@Component
public class ProcessedEvents {

    private static final Pattern SCHEMA = Pattern.compile("[a-z][a-z0-9_]{0,62}");

    private final JdbcTemplate jdbc;

    public ProcessedEvents(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @return {@code true}, если событие для этого потребителя ещё не обрабатывалось (и теперь отмечено);
     *         {@code false} — дубль, обработку нужно пропустить
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(String schema, UUID eventId, String consumer) {
        if (!SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("Недопустимое имя схемы: " + schema);
        }
        int inserted = jdbc.update(
                "INSERT INTO " + schema + ".processed_events (event_id, consumer) VALUES (?, ?) ON CONFLICT DO NOTHING",
                eventId, consumer);
        return inserted == 1;
    }
}
