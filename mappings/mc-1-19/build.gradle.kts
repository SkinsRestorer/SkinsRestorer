tasks {
    remap {
        version.set("1.19")
    }
}

dependencies.apply {
    compileOnly("org.spigotmc:spigot:1.19-R0.1-SNAPSHOT:remapped-mojang@jar") {
        isTransitive = false
    }
    compileOnly("org.spigotmc:spigot-api:1.19-R0.1-SNAPSHOT") {
        isTransitive = false
    }
}
