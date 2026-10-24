package com.apexagro.notification.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.apexagro.notification.SmsApi;

/** BE3-10: заглушка активна везде, кроме prod (DoD: «на prod заглушка отключена»). */
@Configuration(proxyBeanMethods = false)
@Profile("!prod")
@EnableConfigurationProperties(SmsStubProperties.class)
class SmsStubConfig {

    @Bean
    SmsApi stubSmsGateway(SmsStubProperties properties) {
        return new StubSmsGateway(properties);
    }
}
