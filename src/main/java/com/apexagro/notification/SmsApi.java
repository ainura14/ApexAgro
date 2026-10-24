package com.apexagro.notification;

import java.util.Optional;

import com.apexagro.common.i18n.Language;

/**
 * BE3-10: отправка SMS. Публичный API модуля notification.
 *
 * <p>S0 — заглушка (dev/stage), S2 — реальный шлюз (BE3-16, провайдер D-05). На prod заглушки нет:
 * без реального шлюза приложение в prod не стартует.
 *
 * <pre>
 * String code = smsApi.fixedOtpCode(phone).orElseGet(otpGenerator::next);   // BE1-09
 * smsApi.sendOtp(phone, code, user.language());
 * </pre>
 */
public interface SmsApi {

    /**
     * Отправить код подтверждения.
     *
     * @param phone номер в E.164 ({@code +996700123456})
     * @param code  код OTP; в логах и sms_log не сохраняется
     * @param lang  язык текста SMS
     */
    void sendOtp(String phone, String code, Language lang);

    /**
     * Фиксированный код для тестовых номеров (stage и dev, {@code apexagro.sms.stub.test-numbers}).
     * На prod всегда пусто. PROPOSED.
     */
    Optional<String> fixedOtpCode(String phone);
}
