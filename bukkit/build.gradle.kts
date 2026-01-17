import com.github.jengelman.gradle.plugins.shadow.transformers.PreserveFirstFoundResourceTransformer

plugins {
    id("sr.platform-logic")
    alias(libs.plugins.runpaper)
}

base {
    archivesName = "SkinsRestorer-Bukkit"
}

dependencies {
    compileOnly(projects.skinsrestorerShared)
    runtimeOnly(project(":skinsrestorer-shared", "shadow"))
    implementation(projects.multiver.bukkit.shared)
    implementation(projects.multiver.bukkit.paper)
    compileOnly(projects.multiver.miniplaceholders)

    // Explicit mapping dependencies for configuration cache compatibility
    listOf(
        "mc-1-18", "mc-1-18-2",
        "mc-1-19", "mc-1-19-1", "mc-1-19-2", "mc-1-19-3", "mc-1-19-4",
        "mc-1-20", "mc-1-20-2", "mc-1-20-4", "mc-1-20-5",
        "mc-1-21", "mc-1-21-2", "mc-1-21-4", "mc-1-21-5", "mc-1-21-6", "mc-1-21-9", "mc-1-21-11"
    ).forEach { mapping ->
        compileOnly(project(":mappings:$mapping"))
        runtimeOnly(project(":mappings:$mapping", "remapped"))
    }
    testImplementation(testFixtures(projects.test))

    compileOnly(libs.spigot.api) {
        isTransitive = false
    }

    implementation(libs.bstats.bukkit)
    implementation(libs.xseries)

    // MultiPaper support
    implementation(libs.multilib)
    implementation(libs.cloud.paper)

    // PAPI API hook
    compileOnly(libs.placeholderapi) {
        isTransitive = false
    }

    compileOnly(libs.authlib)

    testImplementation(libs.adventure.bukkit)
    testImplementation(libs.spigot.api) {
        isTransitive = false
    }
    testRuntimeOnly(libs.authlib)
}

tasks {
    runServer {
        minecraftVersion(libs.versions.runpaperversion.get())
    }
}

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
        filesNotMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
        transform<PreserveFirstFoundResourceTransformer>()
        exclude("META-INF/annotations.shadow.kotlin_module")
        relocate("net.kyori", "net.skinsrestorer.shadow.kyori")
    }
}
