#!/bin/bash
# macOS entry point. Keep the Windows .cmd/.ps1 launchers independent.
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd -- "$script_dir/../.." && pwd)"
if [[ "$(uname -s)" != Darwin ]]; then
  echo 'This launcher is for macOS. Use the existing Windows launchers on Windows.' >&2
  exit 1
fi
action="${1:-help}"
if [[ "$action" == help || "$action" == --help ]]; then
  cat <<'HELP'
Configure once after installing the pinned tools:
  bash scripts/macos/up-self.sh configure --java-home PATH --maven-home PATH --node-home PATH --mysql-home PATH --redis-home PATH
Then use: check | check-services | install | build | prepare-backend | backend | frontend | open-vscode | redis
Database: db-check | db-init | db-restore --root-client-file PATH [--snapshot PATH]
See docs/Mac环境安装与迁移启动教程.md. No software is downloaded automatically.
HELP
  exit 0
fi
if [[ "$action" == configure ]]; then
  # --node-home can bootstrap configuration without changing the system PATH.
  node_home="${UPSELF_NODE_HOME:-}"
  args=("$@")
  for ((i=1; i<${#args[@]}; i++)); do
    if [[ "${args[$i]}" == --node-home ]]; then
      node_home="${args[$((i+1))]:-}"
    fi
  done
  if [[ -n "$node_home" ]]; then
    node_bin="$node_home/bin/node"
  else
    node_bin="$(command -v node || true)"
  fi
else
  node_file="$project_dir/.local/macos/node-path"
  if [[ ! -f "$node_file" ]]; then
    echo 'Run the Mac configure command first; see docs/Mac环境安装与迁移启动教程.md.' >&2
    exit 1
  fi
  IFS= read -r node_bin < "$node_file"
fi
if [[ -z "${node_bin:-}" || ! -x "$node_bin" ]]; then
  echo 'Node was not found. Install Node 24.22.1 and pass --node-home (the directory containing bin/node).' >&2
  exit 1
fi
exec "$node_bin" "$script_dir/tools.mjs" "$@"
