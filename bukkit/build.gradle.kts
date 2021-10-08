dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")

    api("io.papermc:paperlib:1.0.6")
    api("org.bstats:bstats-bukkit:2.2.1")
    api("co.aikar:acf-paper:0.5.0-SNAPSHOT")
    api("com.github.cryptomorin:XSeries:8.4.0")
    api("co.aikar:minecraft-timings:1.0.4")
    api("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }

    compileOnly("com.viaversion:viabackwards-common:4.0.1")
    compileOnly("com.viaversion:viaversion:4.0.0")
    compileOnly("com.mojang:authlib:1.11")
}