tasks {
    remap {
        version.set("1.19")
    }
}

dependencies.apply {
    compileOnly("org.spigotmc:minecraft-server:1.19-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.19-R0.1-SNAPSHOT:remapped-mojang@jar")
    compileOnly("org.spigotmc:spigot:1.19-R0.1-SNAPSHOT")
}
