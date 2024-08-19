enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.develocity") version "3.18"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "skinsrestorer-parent"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            name = "SpigotMC Repository"
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "PaperMC Repository"
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
        maven("https://maven.wagyourtail.xyz/releases") {
            name = "PaperMC Repository"
        }
        maven("https://maven.wagyourtail.xyz/snapshots") {
            name = "PaperMC Repository"
        }
        mavenCentral()
    }
}

develocity {
    buildScan {
        val isCi = !System.getenv("CI").isNullOrEmpty()
        if (isCi) {
            termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
            termsOfUseAgree = "yes"
            tag("CI")
        }
        publishing.onlyIf { isCi }
    }
}

setOf("shared", "propertyold", "propertynew").forEach {
    include("multiver:bungee:$it")
}

setOf("shared", "v1-7", "spigot", "paper", "folia").forEach {
    include("multiver:bukkit:$it")
}

setOf(
    "1-18", "1-18-2",
    "1-19", "1-19-1", "1-19-2", "1-19-3", "1-19-4",
    "1-20", "1-20-2", "1-20-4", "1-20-5",
    "1-21"
).forEach {
    include("mappings:mc-$it")
}

setupSRSubproject("build-data")
setupSRSubproject("api")
setupSRSubproject("shared")

setupSRSubproject("bukkit")
setupSRSubproject("bungee")
setupSRSubproject("velocity")

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
