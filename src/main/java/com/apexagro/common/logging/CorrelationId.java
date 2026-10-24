package com.apexagro.common.logging;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;

/**
 * BE3-05: сквозной идентификатор запроса. Приходит в заголовке {@code X-Correlation-Id} (или генерируется),
 * лежит в MDC под ключом {@code correlationId}, попадает в каждую строку лога, в ответ, в {@code traceId}
 * ошибок, в события и в заголовки сообщений RabbitMQ. По нему в Loki видна вся цепочка запроса.
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    /** Принимаем только безопасные значения: защита от подделки строк лога через заголовок. */
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    private CorrelationId() {
    }

    public static Optional<String> current() {
        return Optional.ofNullable(MDC.get(MDC_KEY));
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /** Значение из заголовка, если оно корректно; иначе новый id. */
    public static String sanitizeOrGenerate(String candidate) {
        return candidate != null && VALID.matcher(candidate).matches() ? candidate : generate();
    }

    /**
     * Выполняет код с заданным correlation id в MDC и восстанавливает прежнее значение.
     * Используется при обработке событий и сообщений.
     */
    public static void runWith(String correlationId, Runnable action) {
        String previous = MDC.get(MDC_KEY);
        try {
            if (correlationId != null) {
                MDC.put(MDC_KEY, correlationId);
            }
            action.run();
        } finally {
            restore(previous);
        }
    }

    static void restore(String previous) {
        if (previous == null) {
            MDC.remove(MDC_KEY);
        } else {
            MDC.put(MDC_KEY, previous);
        }
    }
}
