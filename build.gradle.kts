import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("SpellCheckingInspection")
plugins {
    kotlin("jvm") version "1.4.10"
    id("se.patrikerdes.use-latest-versions") version "0.2.14"
    id("com.github.ben-manes.versions") version "0.29.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("org.mikeneck.graalvm-native-image") version "0.8.0"
    application
}

group = "com.imgcstmzr"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")

    implementation("com.github.ajalt:clikt:2.8.0")
    implementation("com.github.ajalt:mordant:1.2.1")
    implementation("io.github.config4k:config4k:0.4.2")
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha1")

    implementation("commons-io:commons-io:2.8.0")
    implementation("org.apache.commons:commons-compress:1.20")
    implementation("org.apache.commons:commons-exec:1.3")
    implementation("org.codehaus.plexus:plexus-utils:3.3.0")
    implementation("org.jline:jline-reader:3.16.0")

    @Suppress("SpellCheckingInspection")
    implementation("com.tunnelvisionlabs:antlr4-runtime:4.7.4") // grapheme parsing
    @Suppress("SpellCheckingInspection")
    implementation("com.tunnelvisionlabs:antlr4-perf-testsuite:4.7.4")

    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.platform:junit-platform-console") {
        because("needed to launch the JUnit Platform Console program")
    }
    testImplementation("org.junit.platform:junit-platform-commons")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")

    testImplementation("io.strikt:strikt-core:0.27.0")
    testImplementation("io.strikt:strikt-mockk:0.27.0")
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
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xopt-in=kotlin.InlineClasses",
                "-Xopt-in=kotlin.RequiresOptIn",
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
            includeTestsMatching("com.imgcstmzr.AllTests")
        }
        useJUnitPlatform {
            excludeTags("Slow", "E2E")
        }
    }
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
