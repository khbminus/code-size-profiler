#!/usr/bin/env bash
set -Eeuo pipefail
usage() {
  printf "Usage: %s <ir-sizes left> <ir-sizes right> <output-folder>\n" "$0"
  exit 1
}

./gradlew run --args="diff $1 $2 -o $3/diff-all.html"
./gradlew run --args="diff $1 $2 -o $3/diff-added.html --only-added"
./gradlew run --args="diff $1 $2 -o $3/diff-deleted.html --only-deleted"