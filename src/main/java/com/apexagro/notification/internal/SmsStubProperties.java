package com.apexagro.notification.internal;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * BE3-10: {@code apexagro.sms.stub.*}.
 *
 * @param logCodes    писать OTP-код в лог (только dev)
 * @param fixedCode   код для тестовых номеров
 * @param testNumbers тестовые номера в E.164
 */
@ConfigurationProperties("apexagro.sms.stub")
record SmsStubProperties(boolean logCodes, String fixedCode, List<String> testNumbers) {

    SmsStubProperties {
        fixedCode = fixedCode == null || fixedCode.isBlank() ? "000000" : fixedCode;
        testNumbers = testNumbers == null ? List.of() : List.copyOf(testNumbers);
    }
}
