import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Rimu Markup for the JVM."
version = "11.3.1"
group = "org.rimumarkup"

plugins {
    kotlin("jvm") version "1.6.21"
    application
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.beust:klaxon:5.5")
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

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
