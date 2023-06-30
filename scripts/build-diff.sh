#!/usr/bin/env bash
set -Eeuo pipefail
usage() {
  printf "Usage: %s <ir-sizes left> <dce graph left> <ir-sizes right> <dce graph right> <output-folder>\n" "$0"
  exit 1
}

if [ "$#" -ne 5 ]; then
  usage
fi
mkdir -p "$5/graph-diff"
./gradlew -q run --args="structured-diff $1 $2 $3 $4 -o $5/graph-diff --js"


