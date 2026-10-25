#!/bin/bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if ! /bin/bash "$project_dir/scripts/macos/up-self.sh" open-vscode; then
  echo 'See docs/Mac环境安装与迁移启动教程.md. Press Return to close.'
  read -r ignored
  exit 1
fi
