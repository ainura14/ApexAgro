package com.apexagro.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** BE3-06: профиль пользователя → Accept-Language → язык по умолчанию. */
class ApexLocaleResolverTest {

    private final I18nProperties props = new I18nProperties(List.of(Language.RU, Language.KY), Language.RU);

    private Language resolve(String acceptLanguage, Optional<Language> fromProfile) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (acceptLanguage != null) {
            request.addHeader("Accept-Language", acceptLanguage);
        }
        return new ApexLocaleResolver(props, () -> fromProfile).resolveLanguage(request);
    }

    @Test
    void acceptLanguageIsUsed() {
        assertThat(resolve("ky", Optional.empty())).isEqualTo(Language.KY);
        assertThat(resolve("ky-KG,ru;q=0.8", Optional.empty())).isEqualTo(Language.KY);
        assertThat(resolve("en-US,en;q=0.9,ky;q=0.5", Optional.empty())).isEqualTo(Language.KY);
        assertThat(resolve("ru-RU", Optional.empty())).isEqualTo(Language.RU);
    }

    @Test
    void profileWinsOverHeader() {
        assertThat(resolve("ru", Optional.of(Language.KY))).isEqualTo(Language.KY);
    }

    @Test
    void unsupportedMissingOrMalformedHeaderFallsBackToDefault() {
        assertThat(resolve("en-US", Optional.empty())).isEqualTo(Language.RU);
        assertThat(resolve(null, Optional.empty())).isEqualTo(Language.RU);
        assertThat(resolve(";;;===", Optional.empty())).isEqualTo(Language.RU);
    }
}
