import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("SpellCheckingInspection")
plugins {
    kotlin("jvm") version "1.4.10"
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
    test {
        jvmArgs("-XX:MaxPermSize=256m")
        minHeapSize = "128m"
        maxHeapSize = "512m"
        failFast = false
    }
}

allprojects {
    fun emoji(value: Boolean?): String = when (value) {
        true -> "✅"
        false -> "❌"
        null -> "⭕"
    }

    fun createTestTypeConfigurer(vararg testTypes: String): Test.() -> Unit = {
        useJUnitPlatform {
            println("System properties: ${System.getProperties()}")
            testTypes.forEach { testType ->
                val propertyName = "skip${testType}Tests"
                val propertyValue = System.getProperty(propertyName)
                val skipTestType: Boolean = propertyValue?.let { it == "" || it == "true" } ?: false
                println("Checking $propertyName ... $propertyValue → ${emoji(!skipTestType)}   ")
                if (skipTestType) {
                    excludeTags(testType)
                    systemProperties[propertyName] = "true"
                } else {
                    includeTags(testType)
                    systemProperties[propertyName] = "false"
                }
            }
            check(includeTags.all { !excludeTags.contains(it) } && excludeTags.all { !includeTags.contains(it) }) {
                "includeTage $includeTags and excludeTags $excludeTags must be mutually exclusive!"
            }
            println("Running " + testTypes.joinToString("  ", postfix = "  ") {
                it + " Tests? " + emoji(includeTags.contains(it))
            })
        }
    }

    plugins.withId("java") {
        this@allprojects.tasks {
            val test = "test"(Test::class, createTestTypeConfigurer("Unit", "Integration", "E2E"))
        }
    }

    tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
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
