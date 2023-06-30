#!/usr/bin/env bash
set -Eeuo pipefail

GIT_ROOT="$(git rev-parse --show-toplevel)"
cd "$GIT_ROOT/visualization" && npm install --quiet --no-audit --no-fund