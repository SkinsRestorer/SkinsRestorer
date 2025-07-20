plugins {
    id("sr.platform-logic")
    alias(libs.plugins.runwaterfall)
}

base {
    archivesName = "SkinsRestorer-Bungee"
}

dependencies {
    compileOnly(projects.skinsrestorerShared)
    runtimeOnly(project(":skinsrestorer-shared", "shadow"))
    testImplementation(testFixtures(projects.test))

    compileOnly(libs.bungeecord.api)

    implementation(libs.bstats.bungeecord)
    implementation(libs.cloud.bungee)

    implementation(libs.adventure.bungeecord)
}

tasks {
    runWaterfall {
        version(libs.versions.runwaterfallversion.get())
    }
}

tasks {
    shadowJar {
        relocate("net.kyori", "net.skinsrestorer.shadow.kyori")
    }
}
