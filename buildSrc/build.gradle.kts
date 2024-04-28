plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC Repository"
    }
    maven("https://maven.lenni0451.net/releases") {
        name = "lenni0451MavenReleases"
    }
    maven("https://maven.lenni0451.net/snapshots") {
        name = "lenni0451MavenSnapshots"
    }
}

dependencies {
    implementation("gradle.plugin.com.github.johnrengelman:shadow:8.0.0")
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.0.12")
    implementation("net.kyori:indra-common:3.1.3")
    implementation("net.kyori:indra-git:3.1.3")
    implementation("io.github.patrick.remapper:io.github.patrick.remapper.gradle.plugin:1.4.0")
    implementation("io.freefair.gradle:lombok-plugin:8.6")
    implementation("net.raphimc.java-downgrader:net.raphimc.java-downgrader.gradle.plugin:1.1.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}
