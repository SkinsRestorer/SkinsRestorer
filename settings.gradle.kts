enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.wagyourtail.xyz/releases")
        maven("https://maven.wagyourtail.xyz/snapshots")
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.develocity") version "3.18.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "skinsrestorer-parent"

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

include("modded")
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
