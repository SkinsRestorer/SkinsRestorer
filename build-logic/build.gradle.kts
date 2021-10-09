plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.0")
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
}