#!/usr/bin/env bash
set -Eeuo pipefail
usage() {
  printf "Usage: %s <ir-sizes left> <dce-graph left> <ir-sizes right> <dce-graph right> <output-folder> <name 1> <name 2>\n" "$0"
  exit 1
}

./gradlew -q run --args="diff $1 $3 -o $5/diff-all.html --edge-file $2 --edge-file $4 --name $6 --name $7"
./gradlew -q run --args="diff $1 $3 -o $5/diff-added.html --only-added --edge-file $2 --edge-file $4 --name $6 --name $7"
./gradlew -q run --args="diff $1 $3 -o $5/diff-deleted.html --only-deleted --edge-file $2 --edge-file $4 --name $6 --name $7"