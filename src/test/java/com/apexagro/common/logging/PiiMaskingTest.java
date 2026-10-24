package com.apexagro.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.mask.MaskingJsonGeneratorDecorator;

/** BE3-05 DoD: телефон и ПИН маскируются (и в тексте, и в JSON-логе stage/prod). */
class PiiMaskingTest {

    @Test
    void masksKyrgyzPhonesInAnyFormat() {
        assertThat(PiiMasker.mask("OTP sent to +996700123456")).isEqualTo("OTP sent to +996*******56");
        assertThat(PiiMasker.mask("тел. +996 700 123 456.")).isEqualTo("тел. +996*******56.");
        assertThat(PiiMasker.mask("0700-123-456")).isEqualTo("+996*******56");
        assertThat(PiiMasker.mask("996555987654")).isEqualTo("+996*******54");
    }

    @Test
    void masksPin() {
        assertThat(PiiMasker.mask("ПИН 21234567890123 принят")).isEqualTo("ПИН [PIN] принят");
    }

    @Test
    void leavesOtherNumbersAlone() {
        assertThat(PiiMasker.mask("order 12345, code 482913, amount 1250.00")).isEqualTo("order 12345, code 482913, amount 1250.00");
        assertThat(PiiMasker.mask("id 6f1c2a44-91aa-4f3e-b5d2-0e2f3a4b5c6d")).isEqualTo("id 6f1c2a44-91aa-4f3e-b5d2-0e2f3a4b5c6d");
        assertThat(PiiMasker.mask(null)).isNull();
    }

    @Test
    void maskPhoneForExplicitLogging() {
        assertThat(PiiMasker.maskPhone("+996700123456")).isEqualTo("+996*******56");
    }

    @Test
    void jsonLogIsMasked() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(context);
        MaskingJsonGeneratorDecorator decorator = new MaskingJsonGeneratorDecorator();
        decorator.addValueMasker(new PiiValueMasker());
        decorator.start();
        encoder.setJsonGeneratorDecorator(decorator);
        encoder.start();

        Logger logger = context.getLogger("test");
        LoggingEvent event = new LoggingEvent("fqcn", logger, Level.INFO,
                "user {} pin {}", null, new Object[] {"+996700123456", "11234567890123"});
        String json = new String(encoder.encode(event), StandardCharsets.UTF_8);

        assertThat(json).contains("+996*******56").contains("[PIN]")
                .doesNotContain("700123456").doesNotContain("11234567890123");
        encoder.stop();
    }
}
