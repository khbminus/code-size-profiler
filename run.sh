#!/usr/bin/env bash
set -Eeuo pipefail
usage() {
  printf "Usage: %s [flags] <ir-sizes left (json)> <dce graph left (json)> [<ir-sizes right (json)> <dce graph right (json)>]\n" "$0"
  printf "    -e [arg]    exclude some fqn from dump (could be used several times)\n"
  exit 1
}

declare -a excluded

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
GIT_ROOT="$(git rev-parse --show-toplevel)"
OUTPUT_DATA="$GIT_ROOT/visualization/profile-data"
IR_LEFT="$OUTPUT_DATA/left-graph/ir-sizes.json"
IR_RIGHT="$OUTPUT_DATA/right-graph/ir-sizes.json"
GRAPH_LEFT="$OUTPUT_DATA/left-graph/dce-graph.json"
GRAPH_RIGHT="$OUTPUT_DATA/right-graph/dce-graph.json"
rm -rf "$OUTPUT_DATA/left-graph" "$OUTPUT_DATA/right-graph" "$OUTPUT_DATA/retained-left" "$OUTPUT_DATA/retained-right"

mkdir -p "$OUTPUT_DATA/left-graph" "$OUTPUT_DATA/retained-left"
echo "filtering..."
"$GIT_ROOT/scripts/delete-from-ir" "${excluded[@]}" <"$1" >"$IR_LEFT"
"$GIT_ROOT/scripts/delete-from-edges" "${excluded[@]}" <"$2" >"$GRAPH_LEFT"

if [ "$#" -eq 4 ]; then
  mkdir -p "$OUTPUT_DATA/right-graph" "$OUTPUT_DATA/retained-right"
  "$GIT_ROOT/scripts/delete-from-ir" "${excluded[@]}" <"$3" >"$IR_RIGHT"
  "$GIT_ROOT/scripts/delete-from-edges" "${excluded[@]}" <"$4" >"$GRAPH_RIGHT"
  "$GIT_ROOT/scripts/restore-class-sizes" "$IR_LEFT" "$GRAPH_LEFT"
  "$GIT_ROOT/scripts/restore-class-sizes" "$IR_RIGHT" "$GRAPH_RIGHT"
else
  #  "$GIT_ROOT/scripts/delete-from-ir" "${excluded[@]}" <"$1" >"$IR_RIGHT"
  #  "$GIT_ROOT/scripts/delete-from-edges" "${excluded[@]}" <"$2" >"$GRAPH_RIGHT"
  "$GIT_ROOT/scripts/restore-class-sizes" "$IR_LEFT" "$GRAPH_LEFT"
fi

cd "$GIT_ROOT"

echo "Building dominators tree..."
if [ "$#" -eq 4 ]; then
  "$GIT_ROOT/scripts/build-trees.sh" "$IR_LEFT" "$GRAPH_LEFT" "$IR_RIGHT" "$GRAPH_RIGHT" "$OUTPUT_DATA"
else
  "$GIT_ROOT/scripts/build-trees.sh" "$IR_LEFT" "$GRAPH_LEFT" "$OUTPUT_DATA"
fi

#echo "Building diff of dominator trees..."
#"$GIT_ROOT/scripts/build-retained-diff.sh" "$OUTPUT_DATA"

if [ "$#" -eq 4 ]; then
  echo "Building diff..."
  "$GIT_ROOT/scripts/build-diff.sh" "$IR_LEFT" "$GRAPH_LEFT" "$IR_RIGHT" "$GRAPH_RIGHT" "$OUTPUT_DATA"
  echo "Building diff htmls"
  "$GIT_ROOT/scripts/build-html-diff.sh" "$IR_LEFT" "$GRAPH_LEFT" "$IR_RIGHT" "$GRAPH_RIGHT" "$OUTPUT_DATA" "$1" "$3"
fi
echo "Done."
echo "If you want build mapping from kotlin to wasm please consider to run scripts/generate-source-map script"