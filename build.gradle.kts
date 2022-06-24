import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    application
}

version = "11.1.5"
description = "Rimu Markup for the JVM."

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.beust:klaxon:5.6")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation ("com.github.stefanbirkner:system-lambda:1.2.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("org.rimumarkup.RimuktKt")
}
application.applicationName = "rimukt"  // Set executable script name.
