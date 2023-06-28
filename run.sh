#!/usr/bin/env bash
set -Eeuo pipefail
usage() {
  printf "Usage: %s <ir-sizes left (json)> <dce graph left (json)> <ir-sizes right (json)> <dce graph right (json)>\n" "$0"
  exit 1
}
if [ "$#" -ne 4 ]; then
  usage
fi


GIT_ROOT="$(git rev-parse --show-toplevel)"
IR_LEFT=$(realpath "$1")
IR_RIGHT=$(realpath "$3")
GRAPH_LEFT=$(realpath "$2")
GRAPH_RIGHT=$(realpath "$4")
OUTPUT_DATA="$GIT_ROOT/visualization/src/resources"
cd "$GIT_ROOT"
"$GIT_ROOT/scripts/init.sh"
mkdir -p "$OUTPUT_DATA"
echo "making js of given data..."
echo -n "export const kotlinReachibilityInfos =" > "$OUTPUT_DATA/dce-graph-left.js"
cat "$GRAPH_LEFT" >> "$OUTPUT_DATA/dce-graph-left.js"
echo -n "export const kotlinReachibilityInfos =" > "$OUTPUT_DATA/dce-graph-right.js"
cat "$GRAPH_RIGHT" >> "$OUTPUT_DATA/dce-graph-right.js"
echo -n "export const kotlinDeclarationsSize = " > "$OUTPUT_DATA/ir-sizes-left.js"
cat "$IR_LEFT" >> "$OUTPUT_DATA/ir-sizes-left.js"
echo -n "export const kotlinDeclarationsSize = " > "$OUTPUT_DATA/ir-sizes-right.js"
cat "$IR_RIGHT" >> "$OUTPUT_DATA/ir-sizes-right.js"

echo "Building dominators tree..."
"$GIT_ROOT/scripts/build-trees.sh" "$IR_LEFT" "$GRAPH_LEFT" "$IR_RIGHT" "$GRAPH_RIGHT" "$OUTPUT_DATA"

echo "Building diff..."
"$GIT_ROOT/scripts/build-diff.sh" "$IR_LEFT" "$GRAPH_LEFT" "$IR_RIGHT" "$GRAPH_RIGHT" "$OUTPUT_DATA"

echo "Building diff of dominator trees..."
"$GIT_ROOT/scripts/build-retained-diff.sh" "$OUTPUT_DATA"
cd "$GIT_ROOT/visualization" && npm run build

echo "built all visualization in $GIT_ROOT/visualization/dist"
