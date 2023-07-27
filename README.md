To run this script, you need to generate dumps with the patched compiler (currently branch `dce-info-json-fix`).

How to use:

```shell
$ ./run.sh -e kotlin.test -e <fqn> -e <fqn> <Declaration IR Sizes of left file> \
           <DCE Graph of left file> \
           <Declaration IR Sizes of right file> \
           <DCE Graph of right file>
```

The script also could be used in one-dump mode:

```shell
$ ./run.sh [-e <fqn> [-e <fqn>...]] <IR Dump> <DCE Dump>
```

Use `-e` flag to filter dump for some fqns.

This builds traversable graphs and treemaps (for left and right dumps), treemap of difference,
difference of dominator trees and traversable difference of graphs. Moreover, it generates table of differences.

After it, you can start server with `scripts/server.sh` file.

# Traverse Graph

This page allows seeing the DCE graph structure.
The node is referring to the IR size of the node.
To start:

* check some nodes in the menu on the right (NB: the search bar could
  lag, since it filters with common linear string comparison algorithm)
* Select the desirable maximum depth. Be careful, the high depth may freeze the page.

# Zoomable treemap

The treemap allows seeing the size ratio between your code. All elements of IR dump split by FQN. To go deeper, click on
the element. To go back, click on the name above.

The right panel is used for filtering & choosing the size mode.

# Sourcemap

A page inspired by [Godbolt](https://godbolt.org). The page has two sides: on the left there are several kotlin files (
use the selector to choose the right one). The right side has a WAT text of compiled .wasm file. On hover in colored
area the other side will scroll to chosen color.

To compile this page use `generate-source-map script`:
