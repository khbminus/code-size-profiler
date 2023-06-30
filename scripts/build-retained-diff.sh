#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  printf "Usage: %s <path-to-trees>\n" "$0"
  exit 1
}

if [ "$#" -ne 1 ]; then
  usage
fi
mkdir -p "$1/retained-diff"
./gradlew -q run --args="structured-diff $1/retained-left/retained-sizes.json $1/retained-left/dominator-tree.json $1/retained-right/retained-sizes.json $1/retained-right/dominator-tree.json -o $1/retained-diff --js --tree"
