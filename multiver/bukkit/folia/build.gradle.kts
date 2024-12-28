plugins {
    id("sr.shadow-logic")
}

dependencies {
    implementation(projects.skinsrestorerShared)
    implementation(projects.multiver.bukkit.shared)

    compileOnly("net.kyori:adventure-api:4.18.0")
    compileOnly("dev.folia:folia-api:1.19.4-R0.1-SNAPSHOT") {
        isTransitive = false
    }
}
