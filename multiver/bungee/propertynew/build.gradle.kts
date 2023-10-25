plugins {
    id("sr.base-logic")
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    implementation(projects.multiver.bungee.shared)

    compileOnly("net.md-5:bungeecord-api:1.20-R0.1") {
        isTransitive = false
    }
    compileOnly("net.md-5:bungeecord-proxy:1.19-R0.1-SNAPSHOT") {
        isTransitive = false
    }
    compileOnly("net.md-5:bungeecord-protocol:1.20-R0.1") {
        isTransitive = false
    }
}
