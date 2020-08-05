val kotlin_version: String by project
val ktor_version: String by project
val krossbow_version: String by project


plugins {
    id("org.jetbrains.kotlin.js") version "1.3.72"
}

group = "com.bother-you.reflector"
version = "1.0-SNAPSHOT"

repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")

    implementation("org.hildan.krossbow:krossbow-stomp-core-metadata:$krossbow_version")
    implementation("org.hildan.krossbow:krossbow-stomp-core-js:$krossbow_version")

//    implementation("io.ktor:ktor-client-websockets:$ktor_version")
//    implementation("io.ktor:ktor-client-cio:$ktor_version")
//    implementation("io.ktor:ktor-client-js:$ktor_version")
    implementation(npm("inline-style-prefixer"))
    implementation(npm("iframe"))

    testImplementation(kotlin("test-js"))
}

kotlin {
    target {
        browser {
            distribution {
                directory = File("$projectDir/output/")
                webpackTask {}
            }
        }
    }
}
