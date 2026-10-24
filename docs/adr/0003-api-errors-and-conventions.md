# 0003. Ошибки RFC 7807 и конвенции API

- Статус: Принято (изменения — только по согласованию с командой)
- Дата: 2026-10-24

## Решение
- Любая ошибка — `application/problem+json`: `status`, `title`, `detail` (на языке запроса), `code` (из реестра
  `ErrorCode`), `traceId` (= `X-Correlation-Id`), `errors[]` (только `VALIDATION_FAILED`).
- Коды: `CommonErrorCode` + enum модуля, реализующий `ErrorCode`. Уникальность кодов и наличие текстов ru/ky
  проверяет `ErrorCodeRegistryTest`.
- Язык: профиль пользователя → `Accept-Language` → `apexagro.i18n.default-language` (сейчас `ru`, D-26).
- Деньги — `Money` (строка, 2 знака), количество — `Quantity` (строка, 3 знака); лишние знаки → 400, без округления.
- Даты — `Instant`, ISO 8601 UTC. Страница — `PageResponse { items, page, size, total }`, `page` с нуля.
- Чужой ресурс — 404, а не 403.
