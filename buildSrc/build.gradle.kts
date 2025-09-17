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
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.1.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.2.1")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.4.2")
    implementation("net.kyori:indra-git:3.2.0")
    implementation("io.github.patrick.remapper:io.github.patrick.remapper.gradle.plugin:1.4.2")
    implementation("io.freefair.gradle:lombok-plugin:8.14.2")
    implementation("xyz.wagyourtail.jvmdowngrader:xyz.wagyourtail.jvmdowngrader.gradle.plugin:1.3.3")
    implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:4.3.0")

    implementation("commons-io:commons-io:2.20.0")
    implementation("org.apache.ant:ant:1.10.15")
    implementation("org.codehaus.plexus:plexus-utils:4.0.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}
