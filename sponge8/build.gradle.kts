import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    id("org.spongepowered.gradle.plugin") version "2.0.1"
    id("sr.platform-logic")
}

dependencies {
    compileOnly(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.spongepowered:spongeapi:8.0.0")
    annotationProcessor("org.spongepowered:spongeapi:8.0.0")

    implementation("org.bstats:bstats-sponge:3.0.0")
    implementation("com.github.bloodmc.commands:acf-sponge8:sponge-api8-SNAPSHOT")
}

sponge {
    injectRepositories(false)
    apiVersion("8.0.0")
    license("GPL V3")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("skinsrestorer") {
        displayName("SkinsRestorer")
        entrypoint("net.skinsrestorer.sponge8.SkinsRestorer")
        description(rootProject.description)
        links {
            homepage("https://skinsrestorer.net")
            source("https://github.com/SkinsRestorer/SkinsRestorerX")
            issues("https://github.com/SkinsRestorer/SkinsRestorerX/issues")
        }
        dependency("spongeapi") {
            loadOrder.set(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}
