plugins {
    id("com.gradle.enterprise") version "3.7"
}

rootProject.name = "skinsrestorer"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        //net.kyori.indra.repository.sonatypeSnapshots()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            name = "SpigotMC Repository"
        }
        maven("https://papermc.io/repo/repository/maven-public/") {
            name = "PaperMC Repository"
        }
        maven("https://repo.spongepowered.org/maven") {
            name = "SpongePowered Repository"
        }
        maven("https://repo.codemc.org/repository/maven-public") {
            name = "CodeMC Repository"
        }
        maven("https://repo.aikar.co/content/groups/aikar/") {
            name = "Aikar Repository"
        }
        maven("https://nexus.velocitypowered.com/repository/maven-public/") {
            name = "VelocityPowered Repository"
        }
        maven("https://repo.viaversion.com") {
            name = "ViaVersion Repository"
        }
        maven("https://jitpack.io") {
            name = "JitPack Repository"
        }
        maven("https://libraries.minecraft.net") {
            name = "Minecraft Repository"
        }
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

include("shared")
include("api")
include("bukkit")
include("bungee")
include("velocity")
include("sponge")
