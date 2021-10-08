dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    compileOnly("org.spongepowered:spongeapi:7.3.0")
    annotationProcessor("org.spongepowered:spongeapi:7.3.0")

    api("org.bstats:bstats-sponge:2.2.1")
    api("co.aikar:acf-sponge:0.5.0-SNAPSHOT")
    api("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }
}
