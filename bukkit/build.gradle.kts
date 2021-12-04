dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    implementation(projects.mappings.shared)
    implementation(projects.mappings.mc118)

    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson")
    }

    implementation("io.papermc:paperlib:1.0.6")
    implementation("org.bstats:bstats-bukkit:2.2.1")
    implementation("co.aikar:acf-paper:0.5.0-SNAPSHOT")
    implementation("com.github.cryptomorin:XSeries:8.5.0")
    implementation("co.aikar:minecraft-timings:1.0.4")

    compileOnly("com.viaversion:viabackwards-common:4.0.1")
    compileOnly("com.viaversion:viaversion:4.0.0")
    compileOnly("com.mojang:authlib:1.11")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
}