#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
name="sigaubs-security-schema-check"
image='mysql:8.0.43'
password='schema_validation_only'
trap 'docker rm -f "$name" >/dev/null 2>&1 || true' EXIT

docker rm -f "$name" >/dev/null 2>&1 || true
docker run -d --name "$name" \
    -e MYSQL_ROOT_PASSWORD="$password" \
    -v "$PWD/docker/mysql/01-schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro" \
    -v "$PWD/docker/mysql/02-seed.sql:/docker-entrypoint-initdb.d/02-seed.sql:ro" \
    -v "$PWD/docker/mysql/my.cnf:/etc/mysql/conf.d/sigaubs.cnf:ro" \
    "$image" >/dev/null

ready=0
for _ in $(seq 1 60); do
    if docker logs "$name" 2>&1 | rg -q 'MySQL init process done' \
            && docker exec "$name" mysqladmin ping -h127.0.0.1 -uroot -p"$password" --silent; then
        ready=1
        break
    fi
    sleep 2
done
if test "$ready" != "1"; then
    docker logs "$name" >&2
    echo "MySQL não ficou pronto após aplicar DDL e seed." >&2
    exit 1
fi

result="$(docker exec "$name" mysql -uroot -p"$password" --batch --skip-column-names -e "
USE sigaubs;
SELECT
  (SELECT COUNT(*) FROM patients p JOIN basic_health_units b ON b.id=p.id_basic_health_unit WHERE p.tenant_id<>b.tenant_id)
 + (SELECT COUNT(*) FROM appointments a JOIN patients p ON p.id=a.id_patient WHERE a.tenant_id<>p.tenant_id)
 + (SELECT COUNT(*) FROM contemplations c JOIN medical_slots m ON m.id=c.id_available_medical_slot WHERE c.tenant_id<>m.tenant_id)
 + (SELECT COUNT(*) FROM medical_slots WHERE current_slots<0 OR current_slots>total_slots);
")"
test "$result" = "0"

docker exec "$name" mysql -uroot -p"$password" --batch --skip-column-names -e "
USE sigaubs;
SELECT COUNT(*) > 0 FROM patients;
SELECT COUNT(*) FROM information_schema.tables
 WHERE table_schema='sigaubs' AND table_name IN ('SPRING_SESSION','SPRING_SESSION_ATTRIBUTES');
SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema='sigaubs'
   AND table_name='contemplation_job_executions'
   AND column_name IN ('lock_token','lease_until');
SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
 WHERE table_schema='sigaubs'
   AND table_name='contemplation_job_executions'
   AND index_name='idx_cje_tenant_started';
" | awk '
NR == 1 { if ($1 != 1) exit 1 }
NR == 2 { if ($1 != 2) exit 1 }
NR == 3 { if ($1 != 2) exit 1 }
NR == 4 { if ($1 != 1) exit 1 }
'

echo "Schema, seed e lease do job validados em MySQL limpo."
