tasks {
    remap {
        version.set("1.20.4")
    }
}

dependencies.apply {
    compileOnly("org.spigotmc:spigot:1.20.4-R0.1-SNAPSHOT:remapped-mojang@jar") {
        isTransitive = false
    }
    compileOnly("org.spigotmc:spigot-api:1.20.4-experimental-SNAPSHOT") {
        isTransitive = false
    }
}
