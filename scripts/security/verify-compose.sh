#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
docker compose -f docker-compose-dev.yml config --quiet
MYSQL_ROOT_PASSWORD=validation-root \
MYSQL_USER=sigaubs_app \
MYSQL_PASSWORD=validation-app \
    docker compose -f docker-compose-prd.yml config --quiet

for compose_file in docker-compose-dev.yml docker-compose-prd.yml; do
    rg -q 'read_only: true' "$compose_file"
    rg -q 'cap_drop: \[ALL\]' "$compose_file"
    rg -q 'no-new-privileges:true' "$compose_file"
    rg -q 'internal: true' "$compose_file"
    rg -q 'tmpfs:' "$compose_file"
    rg -Fq 'entrypoint: ["nginx", "-g", "daemon off;"]' "$compose_file"
    rg -Fq '/var/cache/nginx:rw,noexec,nosuid,uid=101,gid=101,mode=0750' "$compose_file"
    rg -Fq '/run:rw,noexec,nosuid,uid=101,gid=101,mode=0750' "$compose_file"
    rg -Fq 'SIGAUBS_CONTEMPLATION_JOB_LEASE_DURATION: ${SIGAUBS_CONTEMPLATION_JOB_LEASE_DURATION:-30m}' "$compose_file"
    published_services="$(awk '
        /^  [a-zA-Z0-9_-]+:$/ { service=$1; sub(":", "", service) }
        /^    ports:$/ { print service }
    ' "$compose_file")"
    if test "$compose_file" = "docker-compose-dev.yml"; then
        test "$published_services" = "gateway"
    else
        test "$published_services" = "$(printf 'gateway\nmysql')"
        rg -Fq '127.0.0.1:${MYSQL_HOST_PORT:-3306}:3306' "$compose_file"
    fi
done

echo "Compose validado: gateway público, MySQL prd somente em loopback e hardening declarado."
