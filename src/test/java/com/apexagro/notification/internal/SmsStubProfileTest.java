package com.apexagro.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.apexagro.common.i18n.Language;
import com.apexagro.notification.SmsApi;

/** BE3-10 DoD: на prod заглушки нет; на dev/stage — есть, тестовые номера получают фиксированный код. */
class SmsStubProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SmsStubConfig.class)
            .withPropertyValues("apexagro.sms.stub.fixed-code=111111",
                    "apexagro.sms.stub.test-numbers=+996700000001");

    @Test
    void noStubOnProd() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean(SmsApi.class));
    }

    @Test
    void stubOnStage() {
        runner.withPropertyValues("spring.profiles.active=stage").run(context -> {
            SmsApi sms = context.getBean(SmsApi.class);
            assertThat(sms.fixedOtpCode("+996700000001")).contains("111111");
            assertThat(sms.fixedOtpCode("+996700999999")).isEmpty();
            sms.sendOtp("+996700999999", "482913", Language.KY);   // не падает, ничего не отправляет
        });
    }
}
