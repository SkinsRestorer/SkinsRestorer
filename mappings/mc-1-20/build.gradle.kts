tasks {
    remap {
        version.set("1.20")
    }
}

dependencies.apply {
    compileOnly("org.spigotmc:spigot:1.20-R0.1-SNAPSHOT:remapped-mojang@jar")
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
}
