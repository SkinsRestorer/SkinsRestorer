tasks {
    remap {
        version.set("1.18")
    }
}

dependencies.apply {
    compileOnly("org.spigotmc:minecraft-server:1.18-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.18-R0.1-SNAPSHOT:remapped-mojang@jar")
    compileOnly("org.spigotmc:spigot:1.18-R0.1-SNAPSHOT")
}
