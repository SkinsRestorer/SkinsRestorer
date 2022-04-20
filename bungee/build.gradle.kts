dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-proxy:1.18-R0.1-SNAPSHOT")

    implementation("org.bstats:bstats-bungeecord:3.0.0")
    implementation("co.aikar:acf-bungee:0.5.1-SNAPSHOT")
}
