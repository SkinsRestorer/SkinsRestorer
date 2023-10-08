plugins {
    id("sr.base-logic")
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    implementation(projects.multiver.bukkit.shared)

    compileOnly("org.bukkit:craftbukkit:1.7.10-R0.1-SNAPSHOT") {
        isTransitive = false
    }
}
