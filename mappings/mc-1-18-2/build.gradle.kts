tasks {
    remap {
        version.set("1.18.2")
    }
}

dependencies.apply {
    compileOnly("io.papermc.paper:paper-server:1.18.2-R0.1-SNAPSHOT:mojang-mapped@jar")
    compileOnly("org.spigotmc:spigot:1.18.2-R0.1-SNAPSHOT")
}
