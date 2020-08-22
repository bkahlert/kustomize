import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.14"
    id("com.github.ben-manes.versions") version "0.29.0"
    application
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("org.mikeneck.graalvm-native-image") version "0.8.0"
}

group = "com.imgcstmzr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha1")
    implementation("com.github.ajalt:clikt:2.8.0")
    implementation("com.github.ajalt:mordant:1.2.1")
    implementation("io.github.config4k:config4k:0.4.2")
    testImplementation(kotlin("test-junit5"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.3.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.2")
    
    testImplementation("io.strikt:strikt-core:0.27.0")
    testImplementation("io.strikt:strikt-mockk:0.27.0")
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.useIR = true
}
application {
    mainClassName = "MainKt"
}

nativeImage {
    graalVmHome = System.getenv("GRAALVM_HOME")
    jarTask = tasks.shadowJar.get()
    mainClass = "MainKt"
    executableName = "imgcstmzr"
    outputDirectory = file("$buildDir/native-image")
    arguments(
        "--no-fallback",
        "--no-server",
        "--enable-all-security-services",
        "--initialize-at-run-time=com.imgcstmzr",
        "--report-unsupported-elements-at-runtime"
    )
}

generateNativeImageConfig {
    enabled = true

    byRunningApplicationWithoutArguments()

    byRunningApplication {
        arguments("-h")
    }

    byRunningApplication {
        arguments("drivers")
    }

}
