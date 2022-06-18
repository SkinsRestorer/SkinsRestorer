dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    implementation(projects.mappings.shared)
    implementation(project(":multiver:paper", "multiverbuild"))
    setOf("1-18", "1-18-2", "1-19").forEach {
        implementation(project(":mappings:mc-$it", "remapped"))
    }

    compileOnly("org.spigotmc:spigot-api:1.19-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson")
    }

    implementation("io.papermc:paperlib:1.0.6")
    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("com.github.cryptomorin:XSeries:8.8.0")

    compileOnly("com.viaversion:viabackwards-common:4.0.1")
    compileOnly("com.viaversion:viaversion:4.0.0")
    compileOnly("com.mojang:authlib:1.11")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
}
