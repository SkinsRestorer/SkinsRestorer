plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.wagyourtail.xyz/releases") {
        name = "WagYourTail Releases Repository"
    }
    maven("https://maven.wagyourtail.xyz/snapshots") {
        name = "WagYourTail Snapshots Repository"
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "Spigot Repository"
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "Paper Repository"
    }
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.6.1")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.0")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.5.10")
    implementation("net.kyori:indra-git:4.1.0")
    implementation("io.freefair.gradle:lombok-plugin:9.5.0")
    implementation("xyz.wagyourtail.jvmdowngrader:xyz.wagyourtail.jvmdowngrader.gradle.plugin:2.0.1")
    implementation("io.papermc.paperweight.userdev:io.papermc.paperweight.userdev.gradle.plugin:2.0.0-beta.21")
    implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:5.1.0")
    implementation("org.openrewrite:plugin:7.39.0")

    implementation("commons-io:commons-io:2.22.0")
    implementation("org.apache.ant:ant:1.10.17")
    implementation("org.codehaus.plexus:plexus-utils:4.0.3")

    // For custom SpigotRemapTask
    implementation("net.md-5:SpecialSource:1.11.6")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}
