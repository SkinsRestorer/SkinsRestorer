plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC Repository"
    }
    maven("https://maven.wagyourtail.xyz/releases") {
        name = "PaperMC Repository"
    }
    maven("https://maven.wagyourtail.xyz/snapshots") {
        name = "PaperMC Repository"
    }
}

dependencies {
    implementation("gradle.plugin.com.github.johnrengelman:shadow:8.0.0")
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.0.19")
    implementation("net.kyori:indra-common:3.1.3")
    implementation("net.kyori:indra-git:3.1.3")
    implementation("io.github.patrick.remapper:io.github.patrick.remapper.gradle.plugin:1.4.2")
    implementation("io.freefair.gradle:lombok-plugin:8.6")
    implementation("xyz.wagyourtail.jvmdowngrader:xyz.wagyourtail.jvmdowngrader.gradle.plugin:0.8.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}
