#!/usr/bin/env bash
# Explicitly invoked future Linux operation. Never stops/restarts the application itself.
set -euo pipefail
umask 077
fail() { printf 'Backup failed: %s
' "$1" >&2; exit 1; }
[[ "$(uname -s)" == Linux && "$EUID" -eq 0 ]] || fail 'Run on reviewed Linux as the backup operator (root).'
for name in systemctl mysqldump mysql openssl tar sha256sum; do command -v "$name" >/dev/null || fail "Missing $name"; done
# This intentionally fails when the app is running: SQL and uploads need one quiet point.
if systemctl is-active --quiet up-self.service; then fail 'Stop up-self.service before this consistent SQL + file backup.'; fi
[[ "$(systemctl show -p ActiveState --value up-self.service)" == inactive ]] || fail 'The service must be installed and fully inactive.'
[[ "$(mysqldump --version)" == *'5.7.44'* ]] || fail 'Use matching MySQL 5.7.44 tools.'
client_file="${UPSELF_BACKUP_CLIENT_FILE:-/etc/up-self/mysql-backup.cnf}"
key_file="${UPSELF_BACKUP_KEY_FILE:-/etc/up-self/backup.key}"
backup_root="${UPSELF_BACKUP_ROOT:-/var/backups/up-self}"
[[ -r "$client_file" && -s "$key_file" && -d /var/lib/up-self/uploads ]] || fail 'Missing client credentials, encryption key or uploads.'
[[ "$backup_root" = /* && "$backup_root" != / ]] || fail 'Use an absolute backup directory.'
mkdir -p -- "$backup_root"
backup_root="$(cd -- "$backup_root" && pwd -P)"
[[ "$(mysql --defaults-extra-file="$client_file" -N -e 'SELECT VERSION()')" == 5.7.44* ]] || fail 'Active MySQL server is not 5.7.44.'
work="$(mktemp -d "$backup_root/.work.XXXXXXXX")"
cleanup() { [[ -n "${work:-}" && "$work" == "$backup_root"/.work.* ]] && rm -rf -- "$work"; }
trap cleanup EXIT
mysqldump --defaults-extra-file="$client_file" --single-transaction --quick --hex-blob   --set-gtid-purged=OFF --no-tablespaces --default-character-set=utf8mb4 up_self_prod > "$work/database.sql"
[[ -s "$work/database.sql" ]] || fail 'Empty database export.'
(cd -- "$work" && sha256sum database.sql > SQL-SHA256SUMS)
name="up-self-$(date -u +%Y%m%dT%H%M%SZ)-$$.tar.gz.enc"
partial="$backup_root/$name.partial"
# Stream uploads into the encrypted archive; never publish the raw dump or keys.
if ! tar -czf - -C "$work" database.sql SQL-SHA256SUMS -C /var/lib/up-self uploads   | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -pass "file:$key_file" -out "$partial"; then
  rm -f -- "$partial"; fail 'Archive or encryption failed.'
fi
mv -- "$partial" "$backup_root/$name"
(cd -- "$backup_root" && sha256sum "$name" > "$name.sha256")
printf 'Encrypted backup created: %s
' "$backup_root/$name"
printf 'Copy encrypted archive + checksum off-host; keep the encryption key separately. No old backups were deleted.
'
