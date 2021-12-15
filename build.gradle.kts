@file:Suppress("SpellCheckingInspection")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.21"
    application
}

group = "com.bkahlert"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {

    implementation("com.bkahlert.kommons:kommons:1.11.2")
//    implementation("com.bkahlert.kommons:kommons:1.12.0-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10") {
        because("filepeek takes 1.3")
    }

    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.5.0-alpha")
    implementation("io.opentelemetry:opentelemetry-sdk:1.5.0")
    implementation("io.opentelemetry:opentelemetry-exporter-jaeger:1.5.0")
    implementation("io.opentelemetry:opentelemetry-exporter-logging:1.5.0")
    implementation("io.grpc:grpc-netty:1.40.1")

    implementation("com.github.ajalt.clikt:clikt:3.1.0")
    implementation("io.github.config4k:config4k:0.4.2")

    implementation("org.apache.commons:commons-compress:1.21") {
        because("needed to extract downloaded archives containing the image")
    }

    testImplementation(platform("org.junit:junit-bom:5.8.0-RC1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-commons")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.platform:junit-platform-console") {
        because("needed to launch the JUnit Platform Console program")
    }

    implementation("io.strikt:strikt-core:0.30.1")
    implementation("io.strikt:strikt-jvm:0.30.1")
    implementation(kotlin("stdlib-jdk8"))
}

tasks {

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            languageVersion = "1.5"
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

    withType<ProcessResources> {
        filesMatching("build.properties") {
            expand(project.properties)
        }
    }

    test {
        useJUnitPlatform()
        testLogging {
            events = setOf(FAILED, STANDARD_OUT, STANDARD_ERROR)
            exceptionFormat = FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
        minHeapSize = "128m"
        maxHeapSize = "512m"
        failFast = false
        ignoreFailures = true
    }
}

application {
    mainClass.set("MainKt")
}
