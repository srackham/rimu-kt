# Rimu Markup for the JVM

_rimu-kt_ is a port of the [Rimu Markup
language](https://srackham.github.io/rimu/) for the Java platform written in
[Kotlin](https://kotlinlang.org/). It is functionally identical to the canonical
[TypeScript implementation](https://github.com/srackham/rimu).

## Using the Rimu library
The Rimu Kotlin library package is can be installed from the
[JitPack](https://jitpack.io/#srackham/rimu-kt) Maven repository.
To use the `rimu-kt` library in a Gradle based project you need to
include the JitPack repository and the `rimu-kt` dependency in your project's
`build.gradle.kts` file e.g.

``` kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.srackham:rimu-kt:11.4.0")
}
```
The above code is taken from the [Minimal Rimu Kotlin
Application](https://github.com/srackham/rimu-kotlin-example) on GitHub.

More examples:

- [Kotlin examples](https://github.com/srackham/rimu-kt/blob/master/src/test/kotlin/KotlinExamplesTest.kt)
- [Java examples](https://github.com/srackham/rimu-kt/blob/master/src/test/java/JavaExamplesTest.java)


## Building rimu-kt
To build from source:

1. Install the Git repository from [Github](https://github.com/srackham/rimu-kt).

        git clone https://github.com/srackham/rimu-kotlin-example.git

2. Build and test:

        ./gradlew clean test

## rimukt CLI
The CLI executable is named `rimukt` and is functionally identical to the
[rimuc](https://srackham.github.io/rimu/reference.html#rimuc-command)
command.

The Gradle `installDist` task builds Rimu CLI command startup scripts: `rimukt`
(for Linux) and `rimukt.bat` (for Windows). These scripts are located in the
`./build/install/rimukt/bin` directory:

    ./gradlew installDist
    ./build/install/rimukt/bin/rimukt --version
    echo '**Hello World!**' | ./build/install/rimukt/bin/rimukt

The last command outputs:

    <p>Hello <em>Rimu</em>!</p>

## Implementation
The one-to-one correspondence between the canonical
[TypeScript code](https://github.com/srackham/rimu) and the Kotlin
code eased porting and debugging.  This also makes it easier to
cross-port new features and bug-fixes.

Both the Kotlin and the TypeScript implementations share the same JSON
driven test suites comprising over 300 compatibility checks.
