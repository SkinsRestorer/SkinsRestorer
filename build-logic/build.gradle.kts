plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/") {
        name = "PaperMC Repository"
    }
}

dependencies {
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.0")
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
    implementation("net.kyori:indra-common:2.0.6")
    implementation("io.papermc.paperweight.userdev:io.papermc.paperweight.userdev.gradle.plugin:1.3.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}