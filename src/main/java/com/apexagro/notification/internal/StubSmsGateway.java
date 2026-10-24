package com.apexagro.notification.internal;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apexagro.common.i18n.Language;
import com.apexagro.common.logging.PiiMasker;
import com.apexagro.notification.SmsApi;

/**
 * BE3-10: заглушка SMS. Ничего не отправляет.
 * dev: пишет код в лог (номер замаскирован). stage: код не пишет; тестовые номера получают фиксированный код.
 */
class StubSmsGateway implements SmsApi {

    private static final Logger log = LoggerFactory.getLogger(StubSmsGateway.class);

    private final SmsStubProperties properties;

    StubSmsGateway(SmsStubProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendOtp(String phone, String code, Language lang) {
        if (properties.logCodes()) {
            // Маскер логов не трогает 4–6-значный код, поэтому код виден разработчику
            log.info("[SMS-заглушка] OTP для {} ({}): {}", PiiMasker.maskPhone(phone), lang.code(), code);
        } else {
            log.info("[SMS-заглушка] OTP для {} ({}) не отправлен: реальный шлюз не подключён",
                    PiiMasker.maskPhone(phone), lang.code());
        }
    }

    @Override
    public Optional<String> fixedOtpCode(String phone) {
        return properties.testNumbers().contains(phone) ? Optional.of(properties.fixedCode()) : Optional.empty();
    }
}
