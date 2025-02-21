plugins {
    id("sr.platform-logic")
    alias(libs.plugins.runwaterfall)
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    implementation(projects.multiver.bungee.shared)
    implementation(projects.multiver.bungee.propertyold)
    implementation(projects.multiver.bungee.propertynew)
    testImplementation(testFixtures(projects.skinsrestorerShared))

    compileOnly("net.md-5:bungeecord-api:1.20-R0.2") {
        isTransitive = false
    }
    compileOnly("net.md-5:bungeecord-proxy:1.18-R0.1-SNAPSHOT")

    implementation("org.bstats:bstats-bungeecord:3.1.0")
    implementation("org.incendo:cloud-bungee:2.0.0-SNAPSHOT")
}

tasks {
    runWaterfall {
        version(libs.versions.runwaterfallversion.get())
    }
}
