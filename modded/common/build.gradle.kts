plugins {
    id("dev.architectury.loom") version "1.6-SNAPSHOT"
    id("architectury-plugin") version "3.4-SNAPSHOT"
}

architectury {
    common("fabric,neoforge".split(','))
}

loom {
    silentMojangMappingsLicense()
}

dependencies {
    // Architectury API. This is optional, and you can comment it out if you don't need it.
    modImplementation("dev.architectury:architectury:13.0.2")

    minecraft("net.minecraft:minecraft:1.21")
    mappings(loom.officialMojangMappings())
}
