enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.lenni0451.net/releases") {
            name = "lenni0451MavenReleases"
        }
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.enterprise") version "3.16.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
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
        maven("https://repo.codemc.org/repository/maven-public/") {
            name = "CodeMC Repository"
        }
        maven("https://repo.codemc.org/repository/nms/") {
            name = "CodeMC NMS Repository"
        }
        maven("https://repo.viaversion.com/") {
            name = "ViaVersion Repository"
        }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
            name = "PlaceholderAPI Repository"
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
        maven("https://repo.opencollab.dev/maven-snapshots/") {
            name = "OpenCollab Snapshot Repository"
        }
        maven("https://repo.opencollab.dev/maven-releases/") {
            name = "OpenCollab Release Repository"
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

setOf(
    "shared",
    "1-18", "1-18-2",
    "1-19", "1-19-1", "1-19-2", "1-19-3", "1-19-4",
    "1-20", "1-20-2", "1-20-4"
).forEach {
    include("mappings:mc-$it")
}

setOf("shared", "propertyold", "propertynew").forEach {
    include("multiver:bungee:$it")
}

setOf("shared", "v1-7", "spigot", "paper", "multipaper", "folia").forEach {
    include("multiver:bukkit:$it")
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
