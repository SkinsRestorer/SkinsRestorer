dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")

    implementation("io.papermc:paperlib:1.0.6")
    implementation("org.bstats:bstats-bukkit:2.2.1")
    implementation("co.aikar:acf-paper:0.5.0-SNAPSHOT")
    implementation("com.github.cryptomorin:XSeries:8.4.0")
    implementation("co.aikar:minecraft-timings:1.0.4")

    compileOnly("com.viaversion:viabackwards-common:4.0.1")
    compileOnly("com.viaversion:viaversion:4.0.0")
    compileOnly("com.mojang:authlib:1.11")
}