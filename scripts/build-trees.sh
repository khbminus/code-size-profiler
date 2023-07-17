#!/usr/bin/env bash
set -Eeuo pipefail
usage() {
  printf "Usage: %s <ir-sizes left> <dce graph left> <ir-sizes right> <dce graph right> <output-folder>\n" "$0"
  exit 1
}
if [ "$#" -eq 5 ]; then
  mkdir -p "$5/retained-left" "$5/retained-right"
  #./gradlew -q run --args="dominators $1 $2 -o $5/retained-left/retained-sizes.json -e $5/retained-left/dominator-tree.json --tree"
  ./gradlew -q run --args="dominators $1 $2 -o $5/retained-left/retained-sizes.json --tree"
  #./gradlew -q run --args="dominators $3 $4 -o $5/retained-right/retained-sizes.json -e $5/retained-right/dominator-tree.json --tree"
  ./gradlew -q run --args="dominators $3 $4 -o $5/retained-right/retained-sizes.json --tree"
else
    mkdir -p "$3/retained-left" "$3/retained-right"
    ./gradlew -q run --args="dominators $1 $2 -o $3/retained-left/retained-sizes.json --tree"
  fi
