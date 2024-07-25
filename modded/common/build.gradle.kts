plugins {
    id("dev.architectury.loom") version "1.6-SNAPSHOT"
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("sr.base-logic")
}

architectury {
    common(listOf("fabric", "neoforge"))
    injectInjectables = false
}

loom {
    silentMojangMappingsLicense()
}

dependencies {
    api(projects.skinsrestorerApi)
    api(projects.skinsrestorerShared)

    api("net.lenni0451.mcstructs:text:2.5.1")
    modImplementation("dev.architectury:architectury:13.0.2")

    minecraft("net.minecraft:minecraft:1.21")
    mappings(loom.officialMojangMappings())
}
