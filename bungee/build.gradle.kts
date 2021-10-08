dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-proxy:1.17-R0.1-SNAPSHOT")

    implementation("org.bstats:bstats-bungeecord:2.2.1")
    implementation("co.aikar:acf-bungee:0.5.0-SNAPSHOT")
}