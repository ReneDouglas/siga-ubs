#!/usr/bin/env bash

set -euo pipefail

compose_file="docker-compose-dev.yml"
env_file=""
database="sigaubs"
mysql_user="root"
mysql_password="${MYSQL_ROOT_PASSWORD:-root}"
tenant_slug=""
tenant_name=""
tenant_domain=""
base_domain="${SIGAUBS_TENANCY_BASE_DOMAIN:-localhost}"
tenant_status="ACTIVE"
admin_username="admin"
admin_name="Administrador Global"
admin_email=""
admin_password=""
admin_password_hash=""

usage() {
  printf '%s\n' "Uso: $0 --slug <tenant> --name <nome> [opções]"
  printf '%s\n' ""
  printf '%s\n' "Opções:"
  printf '%s\n' "  --compose-file <arquivo>       Compose usado para acessar o MySQL (padrão: docker-compose-dev.yml)"
  printf '%s\n' "  --env-file <arquivo>           Arquivo de ambiente para docker compose"
  printf '%s\n' "  --database <nome>              Banco MySQL (padrão: sigaubs)"
  printf '%s\n' "  --mysql-user <usuario>         Usuário MySQL (padrão: root)"
  printf '%s\n' "  --mysql-password <senha>       Senha MySQL (padrão: MYSQL_ROOT_PASSWORD ou root)"
  printf '%s\n' "  --slug <tenant>                Slug do tenant, ex.: afogados"
  printf '%s\n' "  --name <nome>                  Nome do tenant"
  printf '%s\n' "  --base-domain <dominio>        Domínio base para domínio padrão (padrão: SIGAUBS_TENANCY_BASE_DOMAIN ou localhost)"
  printf '%s\n' "  --domain <dominio>             Domínio completo opcional, ex.: afogados.sigaubs.com.br"
  printf '%s\n' "  --status <status>              Status do tenant (padrão: ACTIVE)"
  printf '%s\n' "  --admin-username <usuario>     Usuário admin global (padrão: admin)"
  printf '%s\n' "  --admin-name <nome>            Nome do admin global"
  printf '%s\n' "  --admin-email <email>          Email do admin global"
  printf '%s\n' "  --admin-password <senha>       Senha para gerar BCrypt localmente via htpasswd"
  printf '%s\n' "  --admin-password-hash <hash>   Hash BCrypt já pronto"
}

sql_quote() {
  local value="$1"
  value="${value//\'/\'\'}"
  printf "'%s'" "$value"
}

normalize_slug() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]'
}

generate_bcrypt_hash() {
  local password="$1"

  if ! command -v htpasswd >/dev/null 2>&1; then
    printf '%s\n' "Erro: instale apache2-utils/httpd-tools ou informe --admin-password-hash." >&2
    exit 1
  fi

  local line hash
  line="$(htpasswd -bnBC 10 "" "$password")"
  hash="${line#*:}"
  hash="${hash//$2y$/$2b$}"
  printf '%s' "$hash"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --compose-file) compose_file="$2"; shift 2 ;;
    --env-file) env_file="$2"; shift 2 ;;
    --database) database="$2"; shift 2 ;;
    --mysql-user) mysql_user="$2"; shift 2 ;;
    --mysql-password) mysql_password="$2"; shift 2 ;;
    --slug) tenant_slug="$(normalize_slug "$2")"; shift 2 ;;
    --name) tenant_name="$2"; shift 2 ;;
    --base-domain) base_domain="$(normalize_slug "$2")"; shift 2 ;;
    --domain) tenant_domain="$(normalize_slug "$2")"; shift 2 ;;
    --status) tenant_status="$2"; shift 2 ;;
    --admin-username) admin_username="$2"; shift 2 ;;
    --admin-name) admin_name="$2"; shift 2 ;;
    --admin-email) admin_email="$2"; shift 2 ;;
    --admin-password) admin_password="$2"; shift 2 ;;
    --admin-password-hash) admin_password_hash="$2"; shift 2 ;;
    --help|-h) usage; exit 0 ;;
    *) printf 'Opção desconhecida: %s\n' "$1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$tenant_slug" || -z "$tenant_name" ]]; then
  usage >&2
  exit 1
fi

if [[ ! "$tenant_slug" =~ ^[a-z0-9]([a-z0-9-]*[a-z0-9])?$ ]]; then
  printf 'Slug inválido: %s\n' "$tenant_slug" >&2
  exit 1
fi

if [[ -z "$tenant_domain" ]]; then
  tenant_domain="${tenant_slug}.${base_domain}"
fi

if [[ -z "$admin_password_hash" ]]; then
  if [[ -n "$admin_password" ]]; then
    admin_password_hash="$(generate_bcrypt_hash "$admin_password")"
  else
    printf '%s\n' "Aviso: nenhum hash/senha informado; usando hash de desenvolvimento para admin123." >&2
    admin_password_hash='$2b$10$yHZY3T5CccLi.gG8FhUrtekFVlb7Xuk2yc5Mf6Fj62kre7abaFFWa'
  fi
fi

compose_cmd=(docker compose -f "$compose_file")
if [[ -n "$env_file" ]]; then
  compose_cmd+=(--env-file "$env_file")
fi

tenant_domain_sql="NULL"
if [[ -n "$tenant_domain" ]]; then
  tenant_domain_sql="$(sql_quote "$tenant_domain")"
fi

sql="
INSERT INTO tenants (slug, name, domain, status, creation_date, creation_user)
VALUES ($(sql_quote "$tenant_slug"), $(sql_quote "$tenant_name"), $tenant_domain_sql, $(sql_quote "$tenant_status"), NOW(6), 'bootstrap')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  domain = VALUES(domain),
  status = VALUES(status),
  update_date = NOW(6),
  update_user = 'bootstrap';

INSERT INTO system_admins (username, password, name, email, active, creation_date, creation_user)
VALUES ($(sql_quote "$admin_username"), $(sql_quote "$admin_password_hash"), $(sql_quote "$admin_name"), $(sql_quote "$admin_email"), 1, NOW(6), 'bootstrap')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  name = VALUES(name),
  email = VALUES(email),
  active = 1,
  update_date = NOW(6),
  update_user = 'bootstrap';

SELECT id, slug, name, domain, status FROM tenants WHERE slug = $(sql_quote "$tenant_slug");
SELECT id, username, name, email, active FROM system_admins WHERE username = $(sql_quote "$admin_username");
"

"${compose_cmd[@]}" exec -T mysql mysql -u"$mysql_user" -p"$mysql_password" "$database" <<< "$sql"
