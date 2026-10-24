package com.apexagro.common.i18n;

import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(I18nProperties.class)
class I18nConfig {

    /** Имя бина обязано быть {@code localeResolver}: так его ищет DispatcherServlet. */
    @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    ApexLocaleResolver localeResolver(I18nProperties properties, ObjectProvider<UserLanguageProvider> userLanguage) {
        return new ApexLocaleResolver(properties, () -> {
            UserLanguageProvider provider = userLanguage.getIfAvailable();
            return provider == null ? Optional.empty() : provider.currentUserLanguage();
        });
    }
}
