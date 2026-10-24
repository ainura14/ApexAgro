# 0004. События через Modulith outbox, команды через RabbitMQ

- Статус: Принято (топология RabbitMQ меняется только по согласованию)
- Дата: 2026-10-24

## Решение
- Доменные события (E-xx) публикуются `ApplicationEventPublisher` в транзакции; Spring Modulith сохраняет их
  в `common.event_publication` (outbox) и доставляет `@ApplicationModuleListener` после коммита. Незавершённые
  публикации переотправляются при рестарте.
- Команды (C-xx) — тоже события, помеченные `@Externalized("apexagro.commands::<модуль>.<действие>")`;
  после коммита уходят в RabbitMQ. Очередь `apexagro.cmd.<команда>`, 3 попытки, затем `apexagro.dlx` →
  `apexagro.cmd.<команда>.dlq`. `apexagro.realtime` (topic) — рассылка между экземплярами.
- Каждое событие несёт `EventMetadata` (eventId, occurredAt, correlationId). Обработчики наследуют
  `IdempotentHandler`: `event_id` пишется в `<схема>.processed_events` в той же транзакции.
- Correlation id идёт через MDC, async-задачи (TaskDecorator) и заголовок `X-Correlation-Id` в RabbitMQ.

## Последствия
- Доставка «как минимум один раз» → обработчики обязаны быть идемпотентными.
- DLQ нужно мониторить (алерт — задача QA/DevOps, список метрик от BE3 в S4).
