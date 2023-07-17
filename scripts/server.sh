#!/usr/bin/env bash
set -Eeuo pipefail

GIT_ROOT="$(git rev-parse --show-toplevel)"
"$GIT_ROOT/scripts/init.sh"
cd "$GIT_ROOT"
git submodule update --remote

cd "$GIT_ROOT/visualization"
npm run build
npm start