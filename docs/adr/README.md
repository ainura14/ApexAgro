# Architecture Decision Records

Архитектурные решения проекта. Новое решение — новый файл `NNNN-коротко.md` по шаблону `0000-template.md`,
через PR с апрувом тимлида. Принятые ADR не переписываются: если решение меняется, пишется новый ADR,
а старый получает статус «Заменён ADR-NNNN».

| № | Решение | Статус |
|---|---|---|
| [0001](0001-modular-monolith.md) | Модульный монолит на Spring Modulith, схема БД на модуль | Принято |
| [0002](0002-spring-boot-4.md) | Spring Boot 4.1 вместо Spring Boot 3 из плана | Предложено |
| [0003](0003-api-errors-and-conventions.md) | Ошибки RFC 7807 и конвенции API | Принято |
| [0004](0004-events-and-messaging.md) | События через Modulith outbox, команды через RabbitMQ | Принято |
