# Stage (QA-01)

Один сервер с Docker: приложение + PostgreSQL 16, Redis, RabbitMQ, MinIO и Nginx с HTTPS.
Автодеплой — `.github/workflows/deploy-stage.yml` после зелёного CI на `main`.

## 1. Сервер (один раз, ~30 мин)
Нужно: VPS с Ubuntu 22.04/24.04, 2 vCPU, 4 ГБ RAM, 40 ГБ диска; домен (A-запись `stage.<домен>` → IP сервера).

```bash
# Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER            # перелогиниться

# Файлы stage
sudo mkdir -p /opt/apexagro && sudo chown $USER /opt/apexagro
# скопировать с компьютера содержимое deploy/stage/ в /opt/apexagro (scp -r deploy/stage/* user@server:/opt/apexagro/)
cd /opt/apexagro
cp .env.example .env && nano .env        # домен, образ, пароли (openssl rand -base64 24)
mkdir -p certbot-www

# HTTPS-сертификат Let's Encrypt (порт 80 должен быть свободен)
sudo apt-get install -y certbot
sudo certbot certonly --standalone -d stage.<домен> --agree-tos -m <email> -n
# автопродление: certbot сам ставит таймер; после продления — перезапуск nginx
echo 'deploy-hook = docker restart apexagro-stage-nginx-1' | sudo tee -a /etc/letsencrypt/cli.ini
```

Firewall: открыть только 22, 80, 443 (`sudo ufw allow 22,80,443/tcp && sudo ufw enable`).

## 2. GitHub (один раз)
1. Settings → Secrets and variables → Actions: `STAGE_HOST`, `STAGE_USER`, `STAGE_SSH_KEY`, `STAGE_DOMAIN`.
2. Первый деплой: Actions → deploy-stage → Run workflow. Он соберёт образ в `ghcr.io/<owner>/<repo>:stage`.
3. Если пакет в GHCR приватный — на сервере один раз: `docker login ghcr.io -u <github-user>` (токен с `read:packages`).

## 3. Первый запуск на сервере
```bash
cd /opt/apexagro
docker compose -f docker-compose.stage.yml --env-file .env up -d
docker compose -f docker-compose.stage.yml logs -f app        # дождаться "Started AgroBackendApplication"
curl https://stage.<домен>/actuator/health/readiness           # {"status":"UP"}
```
Swagger: `https://stage.<домен>/swagger-ui.html`. Дальше деплой автоматический при каждом мерже в `main`.

## Доступ к админкам (только через SSH-туннель, наружу не открыты)
```bash
ssh -L 15672:localhost:15672 -L 9001:localhost:9001 -L 5432:localhost:5432 user@server
# RabbitMQ http://localhost:15672, MinIO http://localhost:9001, PostgreSQL localhost:5432 (apexagro / пароль из .env)
```

## Обслуживание
- Логи приложения — JSON в stdout: `docker compose -f docker-compose.stage.yml logs app` (подключение Loki — QA/DevOps).
- Бэкап БД: `docker compose -f docker-compose.stage.yml exec postgres pg_dump -U apexagro apexagro | gzip > backup-$(date +%F).sql.gz`.
- Откат на прошлую версию: в `.env` поставить `APP_IMAGE=ghcr.io/<owner>/<repo>:<sha>` и `up -d app`.
- Сбросить stage начисто: `docker compose ... down -v` (удалит ВСЕ данные).
