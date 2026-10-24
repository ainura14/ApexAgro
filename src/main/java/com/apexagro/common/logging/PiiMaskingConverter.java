package com.apexagro.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/** BE3-05: {@code %maskedMsg} в консольном шаблоне logback — сообщение с замаскированными телефоном и ПИН. */
public class PiiMaskingConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return PiiMasker.mask(event.getFormattedMessage());
    }
}
