pluginManagement {
    repositories {
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
        mavenCentral()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }
}
plugins {
    id("com.gradle.enterprise") version "3.4.1"
}

rootProject.name = "imgcstmzr"
