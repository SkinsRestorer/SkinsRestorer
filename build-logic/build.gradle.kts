plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.0")
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
    implementation("net.kyori:indra-common:2.0.6")
}