#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
release_root="$(cd -- "$script_dir/.." && pwd)"
bash "$script_dir/preflight.sh" --runtime
cd -- "$release_root"
exec "$UPSELF_JAVA_HOME/bin/java" -Dfile.encoding=UTF-8 -Xms256m -Xmx1024m   -jar "$release_root/app/up-self-admin.jar"   --spring.profiles.active=prod   "--spring.config.additional-location=file:${UPSELF_CONFIG_DIR:-/etc/up-self}/"
