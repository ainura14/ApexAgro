package com.apexagro.common.logging;

import com.fasterxml.jackson.core.JsonStreamContext;

import net.logstash.logback.mask.ValueMasker;

/**
 * BE3-05: маскирование в JSON-логе (stage/prod). Подключено в logback-spring.xml через
 * MaskingJsonGeneratorDecorator и применяется ко всем строковым значениям: message, MDC, аргументы, стек.
 */
public class PiiValueMasker implements ValueMasker {

    @Override
    public Object mask(JsonStreamContext context, Object value) {
        if (value instanceof CharSequence text) {
            String original = text.toString();
            String masked = PiiMasker.mask(original);
            return masked.equals(original) ? null : masked;   // null = оставить как есть
        }
        return null;
    }
}
