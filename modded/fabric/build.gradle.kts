plugins {
    id("dev.architectury.loom") version "1.6-SNAPSHOT"
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("sr.base-logic")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    silentMojangMappingsLicense()
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
    modImplementation("net.fabricmc:fabric-loader:0.16.0")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.100.7+1.21")
    modImplementation("dev.architectury:architectury-fabric:13.0.2")

    val cloudFabric = "org.incendo:cloud-fabric:2.0.0-SNAPSHOT"
    modImplementation(cloudFabric)
    include(cloudFabric)

    common(project(":modded:common", "namedElements"))

    minecraft("net.minecraft:minecraft:1.21")
    mappings(loom.officialMojangMappings())
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mapOf(Pair("version", project.version)))
        }
    }
}
