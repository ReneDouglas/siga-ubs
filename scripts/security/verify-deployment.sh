#!/usr/bin/env bash
set -euo pipefail

base_url="${1:?Uso: verify-deployment.sh https://tenant.sigaubs.com.br}"
host="$(printf '%s' "$base_url" | sed -E 's#https?://([^/]+).*#\1#')"

headers="$(mktemp)"
trap 'rm -f "$headers"' EXIT
curl -ksS -D "$headers" -o /dev/null "$base_url/login"

for header in content-security-policy permissions-policy x-content-type-options \
    x-frame-options referrer-policy cache-control; do
    rg -qi "^${header}:" "$headers"
done
rg -qi '^set-cookie: .*HttpOnly' "$headers"
rg -qi '^set-cookie: .*Secure' "$headers"
rg -qi '^set-cookie: .*SameSite=Lax' "$headers"

for port in 3306 8080 9090 2375 2376; do
    if nc -z -w 3 "$host" "$port" >/dev/null 2>&1; then
        echo "Falha: porta pública inesperada $port." >&2
        exit 1
    fi
done

echo "Deployment validado sem registrar conteúdo, cookies ou dados pessoais."
