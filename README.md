To run this script, you need to generate dumps with the patched compiler (currently branch `dce-info-json-fix`).

How to use:

```shell
$ ./run.sh <Declaration IR Sizes of left file> \
           <DCE Graph of left file> \
           <Declaration IR Sizes of right file> \
           <DCE Graph of right file>
```

This builds traversable graphs and treemaps (for left and right dumps), treemap of difference,
difference of dominator trees and traversable difference of graphs. Moreover, it generates table of differences.

After that you will get built html in `visualization/dist` (NB: all links are flatten to dist root, I personally
run `http-server` in `dist/`)