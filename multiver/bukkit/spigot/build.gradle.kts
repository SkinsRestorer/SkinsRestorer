plugins {
    id("sr.base-logic")
}

dependencies {
    implementation(projects.skinsrestorerShared)
    implementation(projects.multiver.bukkit.shared)

    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT") {
        isTransitive = false
    }
}
