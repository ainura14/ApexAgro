package com.apexagro.common.i18n;

import java.util.Locale;
import java.util.Optional;

/** BE3-06: языки интерфейса и сообщений. Код совпадает с {@code identity.users.language}. */
public enum Language {

    KY("ky"),
    RU("ru");

    private final String code;
    private final Locale locale;

    Language(String code) {
        this.code = code;
        this.locale = Locale.forLanguageTag(code);
    }

    public String code() {
        return code;
    }

    public Locale locale() {
        return locale;
    }

    /** {@code "ky"}, {@code "KY"}, {@code "ky-KG"} → KY; неизвестный язык → empty. */
    public static Optional<Language> fromTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return Optional.empty();
        }
        String lang = Locale.forLanguageTag(tag.trim().replace('_', '-')).getLanguage();
        for (Language l : values()) {
            if (l.code.equals(lang)) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }

    public static Optional<Language> fromLocale(Locale locale) {
        return locale == null ? Optional.empty() : fromTag(locale.toLanguageTag());
    }
}
