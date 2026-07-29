#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
image='nginx:1.27.5-alpine'
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

mkdir -p "$tmp_dir/certs/sigaubs.com.br"
openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -subj '/CN=sigaubs.com.br' \
    -keyout "$tmp_dir/certs/sigaubs.com.br/privkey.pem" \
    -out "$tmp_dir/certs/sigaubs.com.br/fullchain.pem" >/dev/null 2>&1

docker run --rm --add-host app:127.0.0.1 \
    -v "$PWD/nginx/dev.conf:/etc/nginx/nginx.conf:ro" \
    "$image" nginx -t
docker run --rm --add-host app:127.0.0.1 \
    -v "$PWD/nginx/prd.conf:/etc/nginx/nginx.conf:ro" \
    -v "$tmp_dir/certs:/etc/nginx/ssl:ro" \
    "$image" nginx -t

rg -q 'listen 80 default_server' nginx/prd.conf
rg -q 'listen 443 ssl default_server' nginx/prd.conf
rg -q 'return 444' nginx/prd.conf
rg -q 'return 301 https://\$redirect_tenant\.sigaubs\.com\.br\$request_uri' nginx/prd.conf
rg -q 'log_format security.*|\$request_method \$uri' nginx/prd.conf
rg -q "script-src 'self'" nginx/prd.conf
rg -q 'Permissions-Policy' nginx/prd.conf
rg -q 'proxy_connect_timeout 5s' nginx/prd.conf
rg -q 'client_max_body_size 1m' nginx/prd.conf
rg -q 'proxy_set_header X-Forwarded-For \$remote_addr' nginx/prd.conf
rg -q 'proxy_set_header X-SIGAUBS-Location-City \$http_cf_ipcity' nginx/prd.conf
rg -q 'proxy_set_header CF-IPLatitude ""' nginx/prd.conf
rg -q 'proxy_set_header X-SIGAUBS-Location-City ""' nginx/dev.conf

echo "nginx validado: sintaxe, Host, logs, localização, proxy e timeouts."
