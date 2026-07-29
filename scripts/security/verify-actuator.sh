#!/usr/bin/env bash
set -euo pipefail

public_url="${PUBLIC_BASE_URL:?Defina PUBLIC_BASE_URL}"
management_url="${MANAGEMENT_BASE_URL:?Defina MANAGEMENT_BASE_URL a partir da rede de gestão}"

public_code="$(curl -ksS -o /dev/null -w '%{http_code}' "$public_url/actuator/health")"
test "$public_code" = "404"

health="$(curl -ksS "$management_url/actuator/health")"
printf '%s' "$health" | jq -e '.status and (keys | sort == ["status"])' >/dev/null
curl -ksSf "$management_url/actuator/prometheus" >/dev/null

echo "Actuator validado: ausente na porta pública e mínimo na porta de gestão."
