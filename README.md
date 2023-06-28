To run this script, you need to generate dumps with the patched compiler (currently branch `dce-info-json-fix`).

How to use:

```shell
$ ./run.sh <Declaration IR Sizes of left file> \
           <DCE Graph of left file> \
           <Declaration IR Sizes of right file> \
           <DCE Graph of right file>
```

After that you will get built html in `visualization/dist` (NB: all links are flatten to dist root, I personally
run `http-server` in `dist/`)

To build a html table of diff, use:
```shell
$ ./gradlew --args="diff <ir sizes.js> <ir sizes2.js>..."
```