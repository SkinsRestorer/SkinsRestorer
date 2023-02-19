enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "PaperMC Repository"
        }
        gradlePluginPortal()
    }
    plugins {
        id("com.github.johnrengelman.shadow") version "7.1.2"
        id("org.cadixdev.licenser") version "0.6.1"
        id("net.kyori.indra") version "3.0.1"
        id("net.kyori.indra.git") version "3.0.1"
        id("net.kyori.indra.publishing") version "3.0.1"
        id("net.kyori.blossom") version "1.3.1"
        id("io.github.patrick.remapper") version "1.4.0"
        id("com.diffplug.spotless") version "6.15.0"
    }
}

plugins {
    id("com.gradle.enterprise") version "3.12.3"
}

rootProject.name = "skinsrestorer-parent"

dependencyResolutionManagement {
    repositories {
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            name = "SpigotMC Repository"
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "PaperMC Repository"
        }
        maven("https://repo.spongepowered.org/maven/") {
            name = "SpongePowered Repository"
        }
        maven("https://nexus.velocitypowered.com/repository/velocity-artifacts-release/") {
            name = "Velocitypowered Repository"
        }
        maven("https://repo.codemc.org/repository/maven-public/") {
            name = "CodeMC Repository"
        }
        maven("https://repo.codemc.org/repository/nms/") {
            name = "CodeMC NMS Repository"
        }
        maven("https://repo.aikar.co/content/groups/aikar/") {
            name = "Aikar Repository"
        }
        maven("https://repo.viaversion.com/") {
            name = "ViaVersion Repository"
        }
        maven("https://jitpack.io/") {
            name = "JitPack Repository"
        }
        maven("https://libraries.minecraft.net/") {
            name = "Minecraft Repository"
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            name = "Sonatype Repository"
        }
        maven("https://repo.clojars.org/") {
            name = "Clojars Repository"
        }
        mavenCentral()
    }
}

gradleEnterprise {
    buildScan {
        if (!System.getenv("CI").isNullOrEmpty()) {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

include("mappings:shared")
setOf("1-18", "1-18-2", "1-19", "1-19-3").forEach {
    include("mappings:mc-$it")
}

setOf("shared", "propertyold", "propertynew").forEach {
    include("multiver:bungee:$it")
}

setOf("v1-7", "spigot", "paper").forEach {
    include("multiver:$it")
}

setupSRSubproject("build-data")
setupSRSubproject("api")
setupSRSubproject("shared")

setupSRSubproject("bukkit")
setupSRSubproject("bungee")
setupSRSubproject("velocity")
setupSRSubproject("sponge")

setupSubproject("skinsrestorer") {
    projectDir = file("universal")
}

fun setupSRSubproject(name: String) {
    setupSubproject("skinsrestorer-$name") {
        projectDir = file(name)
    }
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
