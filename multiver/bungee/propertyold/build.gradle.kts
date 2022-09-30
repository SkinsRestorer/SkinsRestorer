plugins {
    id("sr.base-logic")
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    implementation(projects.multiver.bungee.shared)

    // Keep immport's on older version for SkinApplierBungeeOld
    compileOnly("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-proxy:1.18-R0.1-SNAPSHOT")
}
