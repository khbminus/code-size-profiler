#!/usr/bin/env bash
set -Eeuo pipefail
usage() {
  printf "Usage: %s [flags] <ir-sizes left (json)> <dce graph left (json)> [<ir-sizes right (json)> <dce graph right (json)>]\n" "$0"
  printf "    -e [arg]    exclude some fqn from dump (could be used several times)\n"
  exit 1
}
while getopts "e:" opt; do
  case "$opt" in
  e) excluded+=("$OPTARG") ;;
  ?) echo "invalid flag $OPTARG" && exit 1 ;;
  esac
done

shift $((OPTIND - 1))

if [ "$#" -eq 2 ]; then
  echo -e "\e[33mBuilding in one dump mode...\e[0m"
else
  echo -e "\e[33mBuilding in two dumps mode...\e[0m"
fi
IR_LEFT=$(mktemp)
IR_RIGHT=$(mktemp)
GRAPH_LEFT=$(mktemp)
GRAPH_RIGHT=$(mktemp)
GIT_ROOT="$(git rev-parse --show-toplevel)"
OUTPUT_DATA="$GIT_ROOT/visualization/src/resources"

echo "filtering..."
"$GIT_ROOT/scripts/delete-from-ir" "${excluded[@]}" <"$1" >"$IR_LEFT"
"$GIT_ROOT/scripts/delete-from-edges" "${excluded[@]}" <"$2" >"$GRAPH_LEFT"

if [ "$#" -eq 4 ]; then
  "$GIT_ROOT/scripts/delete-from-ir" "${excluded[@]}" <"$3" >"$IR_RIGHT"
  "$GIT_ROOT/scripts/delete-from-edges" "${excluded[@]}" <"$4" >"$GRAPH_RIGHT"
else
  "$GIT_ROOT/scripts/delete-from-ir" "${excluded[@]}" <"$1" >"$IR_RIGHT"
  "$GIT_ROOT/scripts/delete-from-edges" "${excluded[@]}" <"$2" >"$GRAPH_RIGHT"
fi

cd "$GIT_ROOT"
"$GIT_ROOT/scripts/init.sh"
mkdir -p "$OUTPUT_DATA"
echo "making js of given data..."
echo -n "export const kotlinReachibilityInfos =" >"$OUTPUT_DATA/dce-graph-left.js"
cat "$GRAPH_LEFT" >>"$OUTPUT_DATA/dce-graph-left.js"
echo -n "export const kotlinDeclarationsSize = " >"$OUTPUT_DATA/ir-sizes-left.js"
cat "$IR_LEFT" >>"$OUTPUT_DATA/ir-sizes-left.js"
echo -n "export const kotlinReachibilityInfos =" >"$OUTPUT_DATA/dce-graph-right.js"
cat "$GRAPH_RIGHT" >>"$OUTPUT_DATA/dce-graph-right.js"
echo -n "export const kotlinDeclarationsSize = " >"$OUTPUT_DATA/ir-sizes-right.js"
cat "$IR_RIGHT" >>"$OUTPUT_DATA/ir-sizes-right.js"

echo "Building dominators tree..."
"$GIT_ROOT/scripts/build-trees.sh" "$IR_LEFT" "$GRAPH_LEFT" "$IR_RIGHT" "$GRAPH_RIGHT" "$OUTPUT_DATA"

echo "Building diff..."
"$GIT_ROOT/scripts/build-diff.sh" "$IR_LEFT" "$GRAPH_LEFT" "$IR_RIGHT" "$GRAPH_RIGHT" "$OUTPUT_DATA"

echo "Building diff of dominator trees..."
"$GIT_ROOT/scripts/build-retained-diff.sh" "$OUTPUT_DATA"

if [ "$#" -eq 4 ]; then
echo "Building diff htmls"
"$GIT_ROOT/scripts/build-html-diff.sh" "$IR_LEFT" "$GRAPH_LEFT" "$IR_RIGHT" "$GRAPH_RIGHT" "$OUTPUT_DATA" "$1" "$3"
fi
cd "$GIT_ROOT/visualization"
if [ "$#" -eq 2 ]; then
  cp src/index-one.html src/index.html
else
  cp src/index-two.html src/index.html
fi
npm run build && echo "built all visualization in $GIT_ROOT/visualization/dist"

rm "$IR_LEFT" "$IR_RIGHT" "$GRAPH_LEFT" "$GRAPH_RIGHT"
