package com.apexagro.common.i18n;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * BE3-06: {@code apexagro.i18n.*}.
 *
 * @param supportedLanguages языки, на которые есть бандлы сообщений
 * @param defaultLanguage    язык, если ни профиль, ни заголовок не дали поддерживаемого. [DECISION REQUIRED] D-26
 */
@ConfigurationProperties("apexagro.i18n")
public record I18nProperties(List<Language> supportedLanguages, Language defaultLanguage) {

    public I18nProperties {
        supportedLanguages = supportedLanguages == null || supportedLanguages.isEmpty()
                ? List.of(Language.values())
                : List.copyOf(supportedLanguages);
        defaultLanguage = defaultLanguage == null ? Language.RU : defaultLanguage;
    }
}
