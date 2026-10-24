#!/bin/sh
# BE3-12 seed: бакеты MinIO для разработки. Запускается контейнером minio-init из docker-compose.yml.
# Список бакетов (PROPOSED) согласовать с BE2 (модуль media).
set -e
until mc alias set local http://minio:9000 apexagro apexagro123 >/dev/null 2>&1; do
  echo "Жду MinIO..."; sleep 2
done
for bucket in apexagro-media apexagro-ai-images; do
  mc mb --ignore-existing "local/$bucket"
done
echo "MinIO: бакеты готовы"
