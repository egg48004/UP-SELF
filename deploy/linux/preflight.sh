#!/usr/bin/env bash
set -euo pipefail
fail() { printf 'Preflight failed: %s
' "$1" >&2; exit 1; }
release_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
mode="${1:---artifact-only}"
[[ "$mode" == --artifact-only || "$mode" == --runtime ]] || fail 'Use --artifact-only or --runtime.'
for file in app/up-self-admin.jar web/index.html SHA256SUMS release.json; do
  [[ -f "$release_root/$file" ]] || fail "Missing release artifact: $file"
done
(cd -- "$release_root" && sha256sum --check --status SHA256SUMS) || fail 'Release checksums do not match.'
printf 'Release artifacts and checksums: OK
'
[[ "$mode" == --runtime ]] || exit 0
[[ "$(uname -s)" == Linux && "$(uname -m)" == x86_64 ]] || fail 'Runtime requires reviewed Linux x86_64.'
[[ "${UPSELF_LINUX_RUNTIME_REVIEWED:-no}" == yes ]] || fail 'Linux runtime review is still pending.'
[[ -n "${UPSELF_JAVA_HOME:-}" && -x "$UPSELF_JAVA_HOME/bin/java" ]] || fail 'Set UPSELF_JAVA_HOME.'
java_version="$("$UPSELF_JAVA_HOME/bin/java" -version 2>&1)"
[[ "$java_version" == *'version "1.8.0_231"'* && "$java_version" == *'Java(TM)'* ]] || fail 'Expected the current JDK 8u231; do not substitute another version.'
for command in mysql redis-server; do command -v "$command" >/dev/null || fail "Missing $command"; done
[[ "$(mysql --version)" == *'5.7.44'* ]] || fail 'Expected MySQL client 5.7.44.'
[[ "$(redis-server --version)" == *'v=5.0.14 '* ]] || fail 'Linux Redis candidate must be 5.0.14, not an arbitrary latest release.'
config_dir="${UPSELF_CONFIG_DIR:-/etc/up-self}"
[[ "$config_dir" = /* && -r "$config_dir/application-prod.yml" ]] || fail 'Missing absolute production configuration directory.'
for name in UPSELF_DB_URL UPSELF_DB_USER UPSELF_DB_PASSWORD UPSELF_MIGRATION_USER UPSELF_MIGRATION_PASSWORD UPSELF_REDIS_PASSWORD UPSELF_ADMIN_TOKEN_SECRET UPSELF_STUDENT_TOKEN_SECRET; do
  value="${!name:-}"
  [[ -n "$value" && "$value" != *REPLACE_* ]] || fail "Missing or placeholder value: $name"
done
[[ ${#UPSELF_ADMIN_TOKEN_SECRET} -ge 32 && ${#UPSELF_STUDENT_TOKEN_SECRET} -ge 32 ]] || fail 'Both token secrets must have at least 32 characters.'
[[ "$UPSELF_ADMIN_TOKEN_SECRET" != "$UPSELF_STUDENT_TOKEN_SECRET" ]] || fail 'Login channels need independent secrets.'
[[ "${UPSELF_BOOTSTRAP_ENABLED:-false}" == false || "${UPSELF_BOOTSTRAP_ENABLED:-false}" == true ]] || fail 'Bootstrap must be true or false.'
if [[ "${UPSELF_BOOTSTRAP_ENABLED:-false}" == true ]]; then
  for name in UPSELF_INITIAL_MAINTENANCE_PASSWORD UPSELF_INITIAL_STUDENT_PASSWORD UPSELF_INITIAL_MENTOR_PASSWORD UPSELF_INITIAL_ADMIN_PASSWORD; do
    value="${!name:-}"
    [[ ${#value} -ge 10 && "$value" != *REPLACE_* ]] || fail "Set a real one-time initial password: $name"
  done
fi
[[ -d /var/lib/up-self/uploads && -w /var/lib/up-self/uploads ]] || fail 'Upload directory must already exist and be writable by the service user.'
printf 'Runtime binary/config preflight: OK. Active MySQL/Redis authentication and Linux integration still require smoke testing.
'
