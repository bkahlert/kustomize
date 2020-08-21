import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.14"
    id("com.github.ben-manes.versions") version "0.29.0"
    application
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("org.mikeneck.graalvm-native-image") version "0.8.0"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}
group = "com.imgcstmzr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha1")
    implementation("com.github.ajalt:clikt:2.8.0")
    testImplementation(kotlin("test-junit5"))
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
    outputDirectory = file("bin")
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
