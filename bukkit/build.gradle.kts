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
    implementation(projects.multiver.bukkit.v17)
    compileOnly(projects.multiver.miniplaceholders)

    rootProject.subprojects.forEach {
        if (!it.name.startsWith("mc-")) return@forEach

        compileOnly(project(":mappings:${it.name}"))
        runtimeOnly(project(":mappings:${it.name}", "remapped"))
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

    implementation(libs.adventure.bukkit)

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
        relocate("net.kyori", "net.skinsrestorer.shadow.kyori")
    }
}
