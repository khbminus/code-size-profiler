#!/usr/bin/env bash
set -Eeuo pipefail

GIT_ROOT="$(git rev-parse --show-toplevel)"

echo -e "\e[1;33mBe careful. This script should be run inside code-size-profile.\e[0m"

cd "$GIT_ROOT/visualization" && npm install