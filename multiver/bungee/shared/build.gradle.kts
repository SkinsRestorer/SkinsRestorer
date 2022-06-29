plugins {
    id("sr.base-logic")
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-proxy:1.19-R0.1-SNAPSHOT")
}
