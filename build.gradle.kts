import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.kyori.indra.repository.sonatypeSnapshots

plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.cadixdev.licenser") version "0.6.1"
    id("net.kyori.indra") version "2.0.6"
    id("net.kyori.indra.git") version "2.0.6"
    id("net.kyori.indra.publishing") version "2.0.6"
    id("net.kyori.indra.license-header") version "2.0.6"
    id("net.kyori.blossom") version "1.2.0"
    id("gradle.site") version "0.6"
}

site {
    outputDir.set(file("$rootDir/docs"))
    websiteUrl.set("https://docs.skinsrestorer.net")
    vcsUrl.set("https://github.com/SkinsRestorer/SkinsRestorerX")
}

repositories {
    mavenLocal()
    mavenCentral()
    sonatypeSnapshots()
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.spongepowered.org/maven")
    }
    maven {
        url = uri("https://libraries.minecraft.net")
    }
    maven {
        url = uri("https://repo.codemc.org/repository/maven-public")
    }
    maven {
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }
    maven {
        url = uri("https://repo.velocitypowered.com/snapshots/")
    }
    maven {
        url = uri("https://repo.viaversion.com")
    }
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("net.md-5:bungeecord-config:1.16-R0.4")
    implementation("org.bstats:bstats-bukkit:2.2.1")
    implementation("org.bstats:bstats-bungeecord:2.2.1")
    implementation("org.bstats:bstats-sponge:2.2.1")
    implementation("org.bstats:bstats-velocity:2.2.1")
    implementation("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT")
    implementation("co.aikar:acf-paper:0.5.0-SNAPSHOT")
    implementation("co.aikar:acf-bungee:0.5.0-SNAPSHOT")
    implementation("co.aikar:acf-sponge:0.5.0-SNAPSHOT")
    implementation("com.github.AlexProgrammerDE.commands:acf-velocity:4da0ffec3c")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("mysql:mysql-connector-java:8.0.26")
    implementation("com.github.cryptomorin:XSeries:8.4.0")
    implementation("io.papermc:paperlib:1.0.6")
    implementation("co.aikar:minecraft-timings:1.0.4")
    implementation("com.gilecode.yagson:j9-reflection-utils:1.0")
    implementation("org.spongepowered:configurate-yaml:4.1.2")

    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.16-R0.5-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.16-R0.5-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-proxy:1.16-R0.5-SNAPSHOT")
    compileOnly("org.spongepowered:spongeapi:7.3.0")
    compileOnly("com.velocitypowered:velocity-api:3.0.0")
    compileOnly("com.mojang:authlib:1.11")
    compileOnly("com.viaversion:viabackwards-common:4.0.1")
    compileOnly("com.viaversion:viaversion:4.0.0")

    compileOnly("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.20")
}

group = "net.skinsrestorer"
version = "14.1.4-SNAPSHOT"
description = "Ability to restore/change skins on servers! (Offline and Online Mode)"
java.sourceCompatibility = JavaVersion.VERSION_1_8

indra {
    publishReleasesTo("codemc-releases", "https://repo.codemc.org/repository/maven-releases/")
    publishSnapshotsTo("codemc-snapshots", "https://repo.codemc.org/repository/maven-snapshots")
    github("SkinsRestorer", "SkinsRestorerX")
    gpl3OnlyLicense()
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

blossom {
    replaceToken("{version}", version)
    replaceToken("{description}", rootProject.description)
    replaceToken("{url}", "https://skinsrestorer.net/")
}

tasks {
    processResources {
        expand("version" to version, "description" to description)
    }

    build {
        dependsOn(shadowJar)
    }

    jar {
        enabled = false
    }

    compileJava {
        options.compilerArgs.add("-parameters")
        options.isFork = true
        options.forkOptions.executable = "javac"
    }
}

tasks.create<ConfigureShadowRelocation>("relocateShadowJar") {
    target = tasks["shadowJar"] as ShadowJar
    prefix = "net.skinsrestorer.shadow"
}

tasks.named<ShadowJar>("shadowJar").configure {
    dependsOn(tasks["relocateShadowJar"])
    exclude("META-INF/SPONGEPO.SF", "META-INF/SPONGEPO.DSA", "META-INF/SPONGEPO.RSA")
    minimize()
    archiveFileName.set("SkinsRestorer.jar")
}