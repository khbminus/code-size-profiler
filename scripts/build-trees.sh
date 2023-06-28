#!/usr/bin/env bash
set -Eeuo pipefail
usage() {
  printf "Usage: %s <ir-sizes left> <dce graph left> <ir-sizes right> <dce graph right> <output-folder>\n" "$0"
  exit 1
}

if [ "$#" -ne 5 ]; then
  usage
fi
mkdir -p "$5/retained-left" "$5/retained-right"
./gradlew run --args="dominators $1 $2 -o $5/retained-left/retained-sizes.json -e $5/retained-left/dominator-tree.json --tree"
./gradlew run --args="dominators $1 $2 -o $5/retained-left/retained-sizes.js -e $5/retained-left/dominator-tree.js --tree"
./gradlew run --args="dominators $3 $4 -o $5/retained-right/retained-sizes.json -e $5/retained-right/dominator-tree.json --tree"
./gradlew run --args="dominators $3 $4 -o $5/retained-right/retained-sizes.js -e $5/retained-right/dominator-tree.js --tree"