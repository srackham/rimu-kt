# Rimu Markup for the JVM

_rimu-kt_ is a port of the [Rimu Markup
language](http://rimumarkup.org) for the Java platform written
in Kotlin.


## Features

- Functionally identical to the [JavaScript
  implementation](https://github.com/srackham/rimu).
- The version numbers (starting at 9.1.3) correspond to the
  [JavaScript implementation](https://github.com/srackham/rimu)
  version numbers.
- Includes functionally identical
  [rimuc](http://rimumarkup.org/reference.html#rimuc-command)
  command-line compiler.
- Single runtime dependency: the Kotlin Standard Library.

Details:

- Github: https://github.com/srackham/rimu-kt
- Bintray maven: https://bintray.com/srackham/maven/rimu-kt
- Bintray binaries: https://bintray.com/srackham/generic/rimu-kt#files


## Implementation
The largely one-to-one correspondence between the canonical
[TypeScript code](https://github.com/srackham/rimu) and the Kotlin
code eased porting and debugging.  This will also make it easier to
cross-port new features and bug-fixes.

TypeScript-style namespaces are implemented with Kotlin objects.

Both the Kotlin and JavaScript implementations share the same JSON
driven test suites comprising over 250 compatibility checks.


## Usage
### JVM library
To use the `rimu-kt` library in a Gradle based project you need to
include the _JCenter_ repository and the `rimu-kt` dependency e.g.

```
repositories {
    jcenter()
}

dependencies {
    compile "org.rimumarkup:rimu-kt:9.3.0"
}
```

Example code:

- [Kotlin
  examples](https://github.com/srackham/rimu-kt/blob/master/src/test/kotlin/KotlinExamplesTest.kt).
- [Java
  examples](https://github.com/srackham/rimu-kt/blob/master/src/test/java/JavaExamplesTest.java).

### rimuc compiler command
The `rimu-kt` binary distribution can be downloaded from
[Bintray](https://bintray.com/srackham/generic/rimu-kt#files). Unzip
the binary distribution and run the
[rimuc](http://rimumarkup.org/reference.html#rimuc-command) executable
located in the `./bin` folder.
