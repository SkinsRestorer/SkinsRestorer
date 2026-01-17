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
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.3.1")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.1.0")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.4.8")
    implementation("net.kyori:indra-git:4.0.0")
    implementation("io.freefair.gradle:lombok-plugin:9.2.0")
    implementation("xyz.wagyourtail.jvmdowngrader:xyz.wagyourtail.jvmdowngrader.gradle.plugin:1.3.5")
    implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:4.4.0")

    implementation("commons-io:commons-io:2.21.0")
    implementation("org.apache.ant:ant:1.10.15")
    implementation("org.codehaus.plexus:plexus-utils:4.0.2")

    // For custom SpigotRemapTask
    implementation("net.md-5:SpecialSource:1.11.5")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}
