package com.apexagro.common.i18n;

import java.util.Optional;

/**
 * BE3-06: язык текущего пользователя из профиля. Приоритетнее заголовка {@code Accept-Language}.
 *
 * <p>Реализует модуль identity (BE1) после появления аутентификации: берёт пользователя из
 * SecurityContext и возвращает {@code identity.users.language}. Пока бина нет, язык определяется
 * только по заголовку и значению по умолчанию.
 */
@FunctionalInterface
public interface UserLanguageProvider {

    Optional<Language> currentUserLanguage();
}
