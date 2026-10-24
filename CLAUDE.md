# Правила проекта для ИИ-агентов

Читай `README.md` → раздел «Developer guide»: там структура модуля и все конвенции. Кратко:

- Модульный монолит: Spring Boot 4.1 (Jackson 3: `tools.jackson.*`), Spring Modulith 2.1, Java 21.
- Модуль = пакет под `com.apexagro`; публичный API — в корне модуля, реализация — в `internal/`.
  К другим модулям — только через их `XxxApi` и события. `common` не зависит от бизнес-модулей.
- Миграции: `db/migration/<своя схема>/V<yyyyMMddHHmm>__описание.sql`, без FK на чужие схемы;
  уже применённые миграции не менять — только новые файлы.
- Ошибки: `ApiException` + enum `ErrorCode` модуля + тексты в `messages_ru` и `messages_ky`.
- Деньги `Money`, количество `Quantity`, даты `Instant`, страницы `PageResponse`.
- Эндпоинты — сначала контракт в `api/<module>.yaml`.
- Обработчики событий и команд — наследники `IdempotentHandler`; `@Scheduled` — только с `@SchedulerLock`.
- В логах никаких телефонов, ПИН, OTP, токенов.
- Не менять без апрува тимлида: `common/`, миграции, `application*.yml`, `build.gradle.kts`, CI, тесты в `architecture/`.
- Перед завершением задачи `./gradlew build` должен быть зелёным.
