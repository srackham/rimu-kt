# Rimu Markup for the JVM

_rimu-kt_ is a port of the [Rimu Markup
language](http://rimumarkup.org) for the Java platform written
in Kotlin.


## Features
- Functionally identical to the [JavaScript
  implementation](https://github.com/srackham/rimu) version
  11.1.0 with the following exceptions:

  * Does not support deprecated _Expression macro values_.
  * Does not support deprecated _Imported Layouts_.
  * Because the Go `regexp` package uses RE2 regular expressions there
    are [some limitations](http://rimumarkup.org/reference.html#regular-expressions) on the regular expressions used in
    Replacements definitions and Inclusion/Exclusion macro
    invocations.

Details:

- Github source repo: https://github.com/srackham/rimu-kt
- Bintray maven repo: https://bintray.com/srackham/maven/rimu-kt
- Bintray binaries: https://bintray.com/srackham/generic/rimu-kt#files


## Using the Rimu library
To use the `rimu-kt` library in a Gradle based project you need to
include the _JCenter_ repository and the `rimu-kt` dependency e.g.

``` kotlin
repositories {
    jcenter()
}

dependencies {
    compile "org.rimumarkup:rimu-kt:11.1.0"
}
```

Example code:

- [Kotlin
  examples](https://github.com/srackham/rimu-kt/blob/master/src/test/kotlin/KotlinExamplesTest.kt).
- [Java
  examples](https://github.com/srackham/rimu-kt/blob/master/src/test/java/JavaExamplesTest.java).


## Rimu compiler command
The executable is named `rimukt` and is functionally identical to the
[JavaScript rimuc](http://rimumarkup.org/reference.html#rimuc-command)
command.

The `rimu-kt` binary distribution can be downloaded from
[Bintray](https://bintray.com/srackham/generic/rimu-kt#files). Unzip
the binary distribution and run the `rimukt` executable located in
the `./bin` folder.


## Building Rimu
To build from source:

1. Install the Git repository from [Github](https://github.com/srackham/rimu-kt).

        git clone git@github.com:srackham/rimu-kt.git

2. Build and test:

        ./gradlew test


## Implementation
The largely one-to-one correspondence between the canonical
[TypeScript code](https://github.com/srackham/rimu) and the Kotlin
code eased porting and debugging.  This will also make it easier to
cross-port new features and bug-fixes.

Both the Kotlin and JavaScript implementations share the same JSON
driven test suites comprising over 300 compatibility checks.
