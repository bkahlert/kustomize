import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("SpellCheckingInspection")
plugins {
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.dokka") version "1.4.10"
    id("se.patrikerdes.use-latest-versions") version "0.2.14"
    id("com.github.ben-manes.versions") version "0.29.0"
    application
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("org.mikeneck.graalvm-native-image") version "0.8.0"
}

group = "com.imgcstmzr"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
}
dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha1")
    implementation("com.github.ajalt:clikt:2.8.0")
    implementation("com.github.ajalt:mordant:1.2.1")
    implementation("io.github.config4k:config4k:0.4.2")
    implementation("org.apache.maven.shared:maven-shared-utils:3.3.3")
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.4.10")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.platform:junit-platform-launcher:1.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

    testImplementation("io.strikt:strikt-core:0.27.0")
    testImplementation("io.strikt:strikt-mockk:0.27.0")
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("docs"))
    dokkaSourceSets {
        configureEach {
            samples.from("src/test/com/bkahlert/koodies/string/MatchesKtTest.kt")
        }
    }
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.useIR = true
    kotlinOptions.languageVersion = "1.4"
    @Suppress("SpellCheckingInspection")
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.InlineClasses"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    kotlinOptions.freeCompilerArgs += "-Xinline-classes"
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
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.4"
}
