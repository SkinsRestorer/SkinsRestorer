plugins {
    id("dev.architectury.loom") version "1.6-SNAPSHOT"
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("sr.base-logic")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    silentMojangMappingsLicense()
}

repositories {
    maven("https://maven.neoforged.net/releases") {
        name = "NeoForged"
    }
}

val common = configurations.create("common") {
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
}

dependencies {
    neoForge("net.neoforged:neoforge:21.0.42-beta")
    modImplementation("dev.architectury:architectury-neoforge:13.0.2")

    val cloudNeoForge = "org.incendo:cloud-neoforge:2.0.0-SNAPSHOT"
    modImplementation(cloudNeoForge)
    include(cloudNeoForge)

    common(project(":modded:common", "namedElements"))

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
}
