plugins {
    id("sr.platform-logic")
    alias(libs.plugins.runpaper)
}

base {
    archivesName = "SkinsRestorer-Bukkit"
}

dependencies {
    implementation(project(":skinsrestorer-shared", "shadow"))
    implementation(projects.multiver.bukkit.shared)
    implementation(projects.multiver.bukkit.spigot)
    implementation(projects.multiver.bukkit.paper)
    implementation(projects.multiver.bukkit.v17)
    implementation(projects.multiver.bukkit.folia)
    implementation(projects.multiver.miniplaceholders)

    rootProject.subprojects.forEach {
        if (!it.name.startsWith("mc-")) return@forEach

        compileOnly(project(":mappings:${it.name}"))
        runtimeOnly(project(":mappings:${it.name}", "remapped"))
    }
    testImplementation(testFixtures(projects.test))

    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT") {
        isTransitive = false
    }

    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("com.github.cryptomorin:XSeries:13.3.2")

    // MultiPaper support
    implementation("com.github.puregero:multilib:1.2.4")

    implementation("org.incendo:cloud-paper:2.0.0-SNAPSHOT")

    // PAPI API hook
    compileOnly("me.clip:placeholderapi:2.11.6") {
        isTransitive = false
    }

    compileOnly("com.mojang:authlib:2.0.27")

    testImplementation("org.spigotmc:spigot-api:1.19-R0.1-SNAPSHOT") {
        isTransitive = false
    }
    testRuntimeOnly("com.mojang:authlib:2.0.27")
}

tasks {
    runServer {
        minecraftVersion(libs.versions.runpaperversion.get())
    }
}
