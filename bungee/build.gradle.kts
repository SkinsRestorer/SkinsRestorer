plugins {
    alias(libs.plugins.runwaterfall)
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    implementation(projects.multiver.bungee.shared)
    implementation(projects.multiver.bungee.propertyold)
    implementation(projects.multiver.bungee.propertynew)
    testImplementation(testFixtures(projects.skinsrestorerShared))

    implementation("net.kyori:adventure-platform-bungeecord:4.3.1")

    compileOnly("net.md-5:bungeecord-api:1.20-R0.1") {
        isTransitive = false
    }
    compileOnly("net.md-5:bungeecord-proxy:1.18-R0.1-SNAPSHOT")

    implementation("org.bstats:bstats-bungeecord:3.0.2")
}

tasks {
    shadowJar {
        configureKyoriRelocations()
    }
    runWaterfall {
        version(libs.versions.runwaterfallversion.get())
    }
}
