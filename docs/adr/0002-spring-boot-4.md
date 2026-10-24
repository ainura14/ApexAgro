# 0002. Spring Boot 4.1 вместо Spring Boot 3

- Статус: Предложено (утвердить командой)
- Дата: 2026-10-24

## Контекст
План (BE3-01) указывает Spring Boot 3. Проект создан на Spring Boot 4.1.1 (Spring Framework 7, Spring Modulith 2.1,
Jackson 3, Hibernate 7, Spring Security 7).

## Решение
Остаться на Spring Boot 4.1: проект стартует на актуальной ветке поддержки, миграция с 3 на 4 в середине MVP дороже.

## Последствия
- Jackson 3: пакет `tools.jackson.*` для databind; аннотации остаются `com.fasterxml.jackson.annotation.*`.
- Ретраи RabbitMQ: свойство `spring.rabbitmq.listener.simple.retry.max-retries` (повторы после первой попытки),
  а не `max-attempts` из Boot 3 — старое имя молча игнорируется.
- Часть примеров из интернета для Boot 3 не подходит буквально (пакеты автоконфигураций и тестовых аннотаций
  разнесены по модулям, например `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`).
- Библиотеки — только версии под Spring Framework 7 (ShedLock 7.x, springdoc 3.x).
