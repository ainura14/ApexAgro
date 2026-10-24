# Apex Agro — backend

Модульный монолит: Java 21, Spring Boot 4.1, Spring Modulith 2.1, PostgreSQL 16, Redis, RabbitMQ, MinIO.

## Как запустить (≈10 минут с нуля)

Нужно: **JDK 21** и **Docker Desktop** (запущен).

```bash
git clone https://github.com/Su3r3n/ApexBackend.git
cd ApexBackend
./gradlew bootRun          # Windows: gradlew.bat bootRun
```

`bootRun` сам поднимает `docker-compose.yml` (PostgreSQL, Redis, RabbitMQ, MinIO), применяет миграции
и стартует на http://localhost:8080 с профилем `dev`. Окружение можно поднять и отдельно: `docker compose up -d`.

| Что | Адрес | Логин |
|---|---|---|
| Health | http://localhost:8080/actuator/health | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| RabbitMQ | http://localhost:15672 | apexagro / apexagro |
| MinIO | http://localhost:9001 | apexagro / apexagro123 |
| PostgreSQL | localhost:5432, БД `apexagro` | apexagro / apexagro |

Тесты: `./gradlew test` (нужен запущенный Docker — Testcontainers поднимает свои PostgreSQL, Redis и RabbitMQ).

## Профили

| Профиль | Где | Особенности |
|---|---|---|
| `dev` (по умолчанию) | локально | сервисы из docker compose; текстовые логи; SMS-код пишется в лог; dev-seed |
| `stage` | stage | подключения из переменных окружения; JSON-логи; Swagger открыт; SMS-заглушка с тестовыми номерами |
| `prod` | prod | подключения из переменных окружения; JSON-логи; Swagger закрыт; SMS-заглушки нет |

Переменные окружения stage/prod: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`,
`REDIS_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`,
`SMS_TEST_NUMBERS` (только stage). Запуск: `SPRING_PROFILES_ACTIVE=stage java -jar app.jar`.

Stage-окружение (сервер, Nginx, HTTPS, автодеплой): `deploy/stage/README.md`. Образ приложения — `Dockerfile`.

Probes для Kubernetes/Nginx: `/actuator/health/liveness`, `/actuator/health/readiness` (БД, Redis, RabbitMQ).
Метрики: `/actuator/prometheus` (закрыть от внешнего доступа на уровне Nginx).

---

## Developer guide

### Структура модуля

```
src/main/java/com/apexagro/
├── AgroBackendApplication.java
├── common/                     # общий каркас (BE3), доступен всем модулям
└── <module>/                   # identity, catalog, ordering, delivery, notification, ...
    ├── package-info.java       # @ApplicationModule(allowedDependencies = {"common", ...})
    ├── <Module>Api.java        # публичный API модуля (интерфейс) — только им пользуются другие модули
    ├── <Module>ErrorCode.java  # коды ошибок модуля (enum implements ErrorCode)
    ├── SomethingHappened.java  # события модуля (record ... implements DomainEvent)
    └── internal/               # всё остальное: контроллеры, сервисы, сущности, репозитории
src/main/resources/db/migration/<schema>/V<yyyyMMddHHmm>__описание.sql
api/<module>.yaml               # контракт API модуля
```

Пример — модуль `notification` (`SmsApi` в корне, реализация в `internal/`).

### Правила (проверяются в CI — нарушение ломает сборку)

| Правило | Кто проверяет |
|---|---|
| В чужой `internal` не ходим; чужие репозитории и сущности не используем | `ModularityTests`, `ArchitectureRulesTests` |
| `common` не зависит от бизнес-модулей | `ArchitectureRulesTests` |
| Миграция — только в своей схеме, без FK и ссылок на чужие схемы | `MigrationBoundaryTest` |
| Миграции применяются на пустой БД, имена `V<yyyyMMddHHmm>__...` | `FlywayMigrationsTest` |
| Код ошибки уникален, есть тексты ru и ky | `ErrorCodeRegistryTest` |
| Эндпоинты совпадают с `api/<module>.yaml` | `OpenApiContractTest` (+ openapi-diff в CI) |
| `@Scheduled` только вместе с `@SchedulerLock` | `ArchitectureRulesTests` |

### Как сделать типовые вещи

**Ошибка API** — бросить `ApiException` с кодом своего модуля, текст добавить в `messages_ru.properties` и `messages_ky.properties`:
```java
public enum DeliveryErrorCode implements ErrorCode {
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT);
    ...
}
throw new ApiException(DeliveryErrorCode.INVALID_STATUS_TRANSITION, from, to);   // error.INVALID_STATUS_TRANSITION=Нельзя перейти из {0} в {1}
```
Ответ: RFC 7807 с `code`, `traceId`, `errors[]`. Чужой ресурс → `NotFoundException` (404, не 403).

**DTO**: деньги — `Money`, количество — `Quantity`, даты — `Instant`, страницы — `PageQuery` на входе и `PageResponse` на выходе.

**Таблицы**: новая миграция `db/migration/<схема>/V202611051430__add_xxx.sql`. Применённые миграции не правим.
JPA-сущности указывают схему явно: `@Table(schema = "delivery", name = "shipment_tracking")`.

**Событие** (E-xx) — record с `EventMetadata`, публикация в транзакции:
```java
public record OrderPlaced(EventMetadata metadata, UUID orderId, ...) implements DomainEvent { }
events.publishEvent(new OrderPlaced(EventMetadata.create(), ...));
```
Обработчик — наследник `IdempotentHandler` (нужна таблица `<схема>.processed_events`):
```java
@ApplicationModuleListener
void on(OrderPlaced e) { handleOnce(e, () -> ...); }
```

**Команда через RabbitMQ** (C-xx): `@Externalized(Messaging.COMMANDS_EXCHANGE + "::notification.send-push")` на record команды,
очередь — `@Bean Declarables q() { return Messaging.commandQueue("notification.send-push"); }`,
обработчик — `@RabbitListener(queues = "apexagro.cmd.notification.send-push")` + `@Transactional` + `handleOnce`.
3 попытки, затем DLQ `apexagro.cmd.<команда>.dlq`.

**Лимит запросов**: `rateLimiter.checkOrThrow("otp-send", phone)` — лимиты в `apexagro.rate-limits.policies` (application.yml).

**SMS**: `smsApi.sendOtp(phone, code, lang)`; на dev код виден в логе, тестовые номера — `smsApi.fixedOtpCode(phone)`.

**Задача по расписанию**:
```java
@Scheduled(cron = "${apexagro.delivery.auto-cancel.cron}")
@SchedulerLock(name = "delivery.auto-cancel", lockAtMostFor = "PT5M")
void autoCancel() { ... }
```

**Логи**: телефон и ПИН маскируются автоматически; OTP-коды, токены и пароли не логируем вообще.
Correlation id (`X-Correlation-Id`) есть в каждой строке лога, в ответах, в `traceId` ошибок, в событиях и RabbitMQ.

### Где что настраивать
`application.yml` — общее; `application-<profile>.yml` — по окружениям; `messages/messages_{ru,ky}.properties` — тексты;
`docs/adr/` — архитектурные решения; `.github/CODEOWNERS` — кто ревьюит.
