@file:Suppress("SpellCheckingInspection")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
//    id("se.patrikerdes.use-latest-versions") version "0.2.15"
//    id("com.github.ben-manes.versions") version "0.36.0"
//    id("com.github.johnrengelman.shadow") version "6.0.0"
    application
}

group = "com.imgcstmzr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    mavenLocal()
}

dependencies {

    implementation("com.bkahlert.koodies:koodies:1.2.3")

    implementation("com.christophsturm:filepeek:0.1.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.31") {
        because("filepeek takes 1.3")
    }

    implementation("com.github.ajalt.clikt:clikt:3.1.0")
    implementation("com.github.ajalt:mordant:1.2.1") {// implementation("com.github.ajalt.mordant:mordant:2.0.0-alpha1")
        exclude("org.jetbrains.kotlin", "kotlin-stdlib")
    }
    implementation("io.github.config4k:config4k:0.4.2")
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha1")

    implementation("org.apache.commons:commons-compress:1.20")
    implementation("org.apache.commons:commons-exec:1.3")
    implementation("org.codehaus.plexus:plexus-utils:3.3.0")
    implementation("org.jline:jline-reader:3.16.0")

    implementation("com.tunnelvisionlabs:antlr4-runtime:4.9.0") // grapheme parsing
    implementation("com.tunnelvisionlabs:antlr4-perf-testsuite:4.9.0")

    testImplementation(platform("org.junit:junit-bom:5.8.0-M1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-commons")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.platform:junit-platform-console") {
        because("needed to launch the JUnit Platform Console program")
    }

    testImplementation("io.strikt:strikt-core:0.28.2")
    testImplementation("io.strikt:strikt-mockk:0.29.0")
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    dependencyUpdates {
        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            useIR = true
            languageVersion = "1.4"
            freeCompilerArgs = listOf(
                "-Xjvm-default=all",
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
                "-Xopt-in=kotlin.time.ExperimentalTime",
                "-Xopt-in=kotlin.contracts.ExperimentalContracts",
                "-Xopt-in=kotlin.io.path.ExperimentalPathApi",
                "-Xinline-classes"
            )
        }
    }

    test {
        minHeapSize = "128m"
        maxHeapSize = "512m"
        failFast = false
        ignoreFailures = true
        filter {
            includeTestsMatching("com.imgcstmzr.Tests")
        }
        useJUnitPlatform {
            excludeTags("Slow", "E2E")
        }
    }
}

application {
    mainClassName = "MainKt"
}
