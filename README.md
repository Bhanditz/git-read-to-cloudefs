Attempt at reading directly from a git repository to cloudEFS.

# Building

To build, you will first need to build and package fejoa.

Clone this repo into a new directory.

Then, you need to copy the needed jars from fejoa over to the `unmanaged_jars` directory of this project. To do this, you could from the fejoa directory, run something like:
```
find . -iname *.jar -not -iname cli-* -exec cp \{\} ../git-read-to-cloudefs/unmanaged_jars/ \;
```

Then, to build, run `gradle --no-daemon clean build`.
