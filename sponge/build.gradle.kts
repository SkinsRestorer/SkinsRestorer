import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    id("org.spongepowered.gradle.plugin") version "2.1.1"
}

dependencies {
    compileOnly(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    testImplementation(testFixtures(projects.skinsrestorerShared))

    implementation("org.bstats:bstats-sponge:3.0.2")
    compileOnly("com.mojang:authlib:1.11")
}

sponge {
    apiVersion("8.1.0")
    license("GNU GENERAL PUBLIC LICENSE Version 3")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    injectRepositories(false)
    plugin("skinsrestorer") {
        displayName("SkinsRestorer")
        entrypoint("net.skinsrestorer.sponge.SRSpongeBootstrap")
        description(rootProject.description)
        links {
            homepage("https://skinsrestorer.net")
            source("https://github.com/SkinsRestorer/SkinsRestorerX")
            issues("https://github.com/SkinsRestorer/SkinsRestorerX/issues")
        }
        contributor("knat") {
        }
        contributor("AlexProgrammerDE") {
        }
        contributor("Blackfire62") {
        }
        contributor("McLive") {
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}
