#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

npm ci
npm run build:postcss

if rg -n --pcre2 '<script(?![^>]*\bsrc\s*=)|\son[a-z]+\s*=|hx-on|js:|(?:src|href)\s*=\s*["'\'']https?://' \
    src/main/jte; then
    echo "Falha: template contém JavaScript inline ou recurso externo." >&2
    exit 1
fi

if rg -n '\.innerHTML|\.outerHTML|insertAdjacentHTML|document\.write' \
    src/main/resources/static/js -g '*.js' -g '!*.min.js'; then
    echo "Falha: código JavaScript próprio contém sink HTML inseguro." >&2
    exit 1
fi

echo "Frontend validado: build e política sem JavaScript inline."
