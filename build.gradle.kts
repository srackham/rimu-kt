import java.util.Properties
import java.io.FileInputStream
import com.jfrog.bintray.gradle.BintrayExtension

group = "org.rimumarkup"
version = "11.1.1"
description = "Rimu Markup for the JVM."

val secretProperties = Properties()
if (File("./secret.properties").isFile()) {
    secretProperties.load(FileInputStream("./secret.properties"))
} else {
    // Default values.
    secretProperties["bintray.user"] = ""
    secretProperties["bintray.apikey"] = ""
}

application {
    mainClassName = "org.rimumarkup.RimuktKt"
    applicationName = "rimu-kt"
}

plugins {
    application
    kotlin("jvm").version("1.3.31")
    java
    `maven-publish`
    id("com.jfrog.bintray").version("1.8.4")
}

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib"))
    testCompile(kotlin("reflect"))
    testCompile("junit:junit:4.12")
    testCompile("com.github.stefanbirkner:system-rules:1.19.0")
    testCompile("com.beust:klaxon:5.0.5")
}

tasks.startScripts {
    applicationName = "rimukt" // Set name of executable Rimu compile command in binary distribution.
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource).exclude("**/*.rmu")     // Exclude resource files.
}

publishing {
    publications {
        create<MavenPublication>("rimu") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
}

// Used by bintrayUpload task publishes Maven library to Bintray.
// See https://github.com/bintray/gradle-bintray-plugin
bintray {
    setPublications("rimu") // Name of MavenPublication object.
    user = secretProperties["bintray.user"] as String
    key = secretProperties["bintray.apikey"] as String
    dryRun = true
    publish = false // Manually publish rimu-kt files from the Bintray maven repo browser UI at https://bintray.com/srackham/maven
    override = false // Whether to override version artifacts already published.
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven" // Existing repository in bintray to add the artifacts to (for example: 'generic', 'maven').
        name = application.applicationName
        desc = project.description
        setLicenses("MIT")
        setLabels("rimu", "markup", "asciidoc", "markdown")
        vcsUrl = "https://github.com/srackham/rimu-kt"
        websiteUrl = "https://srackham.github.io/rimu"
        issueTrackerUrl = "https://github.com/srackham/rimu/issues"
        version.name = project.version as String
        publicDownloadNumbers = false
    })
}

