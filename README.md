# Rimu Markup for the JVM

_rimu-kt_ is a port of the [Rimu Markup
language](http://rimumarkup.org) for the Java platform written
in Kotlin.


## Features

- Functionally identical to the [JavaScript
  implementation](https://github.com/srackham/rimu) version
  10.4.2 with the following exceptions:
  
  * Does not support _Expression macro values_.
  
- Includes
  [rimuc](http://rimumarkup.org/reference.html#rimuc-command)
  command-line compiler.
- Single runtime dependency: the Kotlin Standard Library.

Details:

- Github source repo: https://github.com/srackham/rimu-kt
- Bintray maven repo: https://bintray.com/srackham/maven/rimu-kt
- Bintray binaries: https://bintray.com/srackham/generic/rimu-kt#files


## Using the Rimu library
To use the `rimu-kt` library in a Gradle based project you need to
include the _JCenter_ repository and the `rimu-kt` dependency e.g.

```
repositories {
    jcenter()
}

dependencies {
    compile "org.rimumarkup:rimu-kt:10.4.2"
}
```

Example code:

- [Kotlin
  examples](https://github.com/srackham/rimu-kt/blob/master/src/test/kotlin/KotlinExamplesTest.kt).
- [Java
  examples](https://github.com/srackham/rimu-kt/blob/master/src/test/java/JavaExamplesTest.java).


## rimuc compiler command
The `rimu-kt` binary distribution can be downloaded from
[Bintray](https://bintray.com/srackham/generic/rimu-kt#files). Unzip
the binary distribution and run the
[rimuc](http://rimumarkup.org/reference.html#rimuc-command) executable
located in the `./bin` folder.


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

TypeScript-style namespaces are implemented with Kotlin objects.

Both the Kotlin and JavaScript implementations share the same JSON
driven test suites comprising over 250 compatibility checks.

