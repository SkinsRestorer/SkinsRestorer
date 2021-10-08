dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-proxy:1.17-R0.1-SNAPSHOT")

    api("org.bstats:bstats-bungeecord:2.2.1")
    api("co.aikar:acf-bungee:0.5.0-SNAPSHOT")
    api("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }
}