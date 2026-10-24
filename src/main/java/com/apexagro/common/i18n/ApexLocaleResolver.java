package com.apexagro.common.i18n;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.LocaleResolver;

/**
 * BE3-06: порядок выбора языка ответа.
 * <ol>
 *   <li>язык из профиля пользователя ({@link UserLanguageProvider}, если модуль identity его предоставил);</li>
 *   <li>первый поддерживаемый язык из {@code Accept-Language} (с учётом q-весов);</li>
 *   <li>язык по умолчанию ({@code apexagro.i18n.default-language}).</li>
 * </ol>
 */
public class ApexLocaleResolver implements LocaleResolver {

    private final I18nProperties properties;
    private final Supplier<Optional<Language>> userLanguage;

    public ApexLocaleResolver(I18nProperties properties, Supplier<Optional<Language>> userLanguage) {
        this.properties = properties;
        this.userLanguage = userLanguage;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return resolveLanguage(request).locale();
    }

    public Language resolveLanguage(HttpServletRequest request) {
        Optional<Language> fromProfile = userLanguage.get().filter(properties.supportedLanguages()::contains);
        if (fromProfile.isPresent()) {
            return fromProfile.get();
        }
        return fromAcceptLanguage(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE))
                .orElse(properties.defaultLanguage());
    }

    Optional<Language> fromAcceptLanguage(String header) {
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        try {
            for (Locale.LanguageRange range : Locale.LanguageRange.parse(header)) {
                if (range.getWeight() <= 0) {
                    continue;
                }
                Optional<Language> language = Language.fromTag(range.getRange())
                        .filter(properties.supportedLanguages()::contains);
                if (language.isPresent()) {
                    return language;
                }
            }
        } catch (IllegalArgumentException malformedHeader) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException(
                "Язык задаётся профилем пользователя или заголовком Accept-Language");
    }
}
