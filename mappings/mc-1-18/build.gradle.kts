tasks {
    remap {
        version.set("1.18")
    }
}

dependencies.apply {
    compileOnly("org.spigotmc:spigot:1.18-R0.1-SNAPSHOT:remapped-mojang@jar") {
        isTransitive = false
    }
    compileOnly("org.spigotmc:spigot-api:1.18-rc3-R0.1-SNAPSHOT") {
        isTransitive = false
    }
}
