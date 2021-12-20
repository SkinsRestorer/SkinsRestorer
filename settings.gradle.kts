enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        maven("https://papermc.io/repo/repository/maven-public/") {
            name = "PaperMC Repository"
        }
        gradlePluginPortal()
    }
    plugins {
        id("com.github.johnrengelman.shadow") version "7.1.1"
        id("org.cadixdev.licenser") version "0.6.1"
        id("net.kyori.indra") version "2.0.6"
        id("net.kyori.indra.git") version "2.0.6"
        id("net.kyori.indra.publishing") version "2.0.6"
        id("net.kyori.blossom") version "1.3.0"
        id("io.papermc.paperweight.userdev") version "1.3.1"
    }
}

plugins {
    id("com.gradle.enterprise") version "3.8"
}

rootProject.name = "skinsrestorer-parent"

dependencyResolutionManagement {
    repositories {
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            name = "SpigotMC Repository"
        }
        maven("https://papermc.io/repo/repository/maven-public/") {
            name = "PaperMC Repository"
        }
        maven("https://repo.spongepowered.org/maven/") {
            name = "SpongePowered Repository"
        }
        maven("https://nexus.velocitypowered.com/repository/maven-public/") {
            name = "VelocityPowered Repository"
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

includeBuild("build-logic")

include("mappings:shared")
setOf("1-18").forEach {
    include("mappings:mc-$it")
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
