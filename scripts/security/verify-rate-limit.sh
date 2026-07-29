#!/usr/bin/env bash
set -euo pipefail

base_url="${SECURITY_BASE_URL:?Defina SECURITY_BASE_URL, por exemplo https://tenant.sigaubs.com.br}"
tenant_host="${SECURITY_TENANT_HOST:?Defina SECURITY_TENANT_HOST}"
other_host="${SECURITY_OTHER_TENANT_HOST:?Defina SECURITY_OTHER_TENANT_HOST}"

burst() {
    local host="$1"
    local path="$2"
    local total="$3"
    local got_429=0
    for _ in $(seq 1 "$total"); do
        code="$(curl -ksS -o /dev/null -w '%{http_code}' \
            -H "Host: $host" -X POST "$base_url$path")"
        if test "$code" = "429"; then
            got_429=1
        fi
    done
    test "$got_429" = "1"
}

burst "$tenant_host" /login 12
burst "$tenant_host" /systemUser-management/validate 18

# A chave inclui o hostname: a rajada anterior não deve consumir a cota do outro tenant.
other_code="$(curl -ksS -o /dev/null -w '%{http_code}' \
    -H "Host: $other_host" -X POST "$base_url/login")"
test "$other_code" != "429"

echo "Rate limits validados, inclusive isolamento por hostname."
