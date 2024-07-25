plugins {
    id("com.github.johnrengelman.shadow")
    id("dev.architectury.loom") version "1.6-SNAPSHOT"
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("sr.base-logic")
}

architectury {
    platformSetupLoomIde()
    neoForge()
    injectInjectables = false
}

loom {
    silentMojangMappingsLicense()
}

repositories {
    maven("https://maven.neoforged.net/releases") {
        name = "NeoForged"
    }
}

val common: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val developmentNeoForge: Configuration = configurations.getByName("developmentNeoForge")
val shadowBundle: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

configurations {
    compileClasspath {
        extendsFrom(common)
    }
    runtimeClasspath {
        extendsFrom(common)
    }
    developmentNeoForge.extendsFrom(common)
}

dependencies {
    neoForge("net.neoforged:neoforge:21.0.42-beta")
    modImplementation("dev.architectury:architectury-neoforge:13.0.2")

    val cloudNeoForge = "org.incendo:cloud-neoforge:2.0.0-SNAPSHOT"
    modImplementation(cloudNeoForge)
    include(cloudNeoForge)

    common(project(":modded:common", "namedElements"))
    shadowBundle(project(":modded:common", "transformProductionNeoForge"))

    minecraft("net.minecraft:minecraft:1.21")
    mappings(loom.officialMojangMappings())
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mapOf(Pair("version", project.version)))
        }
    }

    shadowJar {
        configurations = listOf(shadowBundle)
        archiveClassifier = "dev-shadow"
    }

    remapJar {
        inputs.files(shadowJar.get().archiveFile)
    }
}
