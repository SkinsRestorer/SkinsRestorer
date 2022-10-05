plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC Repository"
    }
}

dependencies {
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
    implementation("net.kyori:indra-common:3.0.0")
    implementation("io.github.patrick.remapper:io.github.patrick.remapper.gradle.plugin:1.3.0")
}

java.targetCompatibility = JavaVersion.VERSION_17
