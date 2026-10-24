# Dev seed

Тестовые данные только для профиля `dev` (подключается в `application-dev.yml`, в stage/prod не попадает).

- Файлы — повторяемые миграции Flyway: `R__<схема>_<что>.sql`, например `R__reference_regions.sql`.
- Пишите идемпотентно: `INSERT ... ON CONFLICT DO NOTHING`.
- Справочники для всех окружений (D-16) — не сюда, а в `db/migration/<схема>/` модуля-владельца.
