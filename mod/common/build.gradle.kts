plugins {
    id("dev.architectury.loom") version "1.13-SNAPSHOT"
    id("sr.base-logic")
}

base {
    archivesName = "SkinsRestorer-Mod-Common"
}

loom {
    silentMojangMappingsLicense()

    accessWidenerPath = file("src/main/resources/skinsrestorer.accesswidener")
}

dependencies {
    minecraft("net.minecraft:minecraft:${rootProject.property("modMcVersion")}")
    mappings(loom.officialMojangMappings())

    // We depend on Fabric Loader here to use the Fabric @Environment annotations,
    // which get remapped to the correct annotations on each platform.
    // Do NOT use other classes from Fabric Loader.
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")

    // Architectury API. This is optional, and you can comment it out if you don't need it.
    modImplementation("dev.architectury:architectury:${rootProject.property("architectury_version")}")

    // Shared project dependencies
    setOf(
        projects.skinsrestorerShared,
        projects.multiver.miniplaceholders,
        projects.multiver.viaversion
    ).forEach {
        implementation(it) {
            exclude("com.google.code.gson")
            exclude("com.google.errorprone")
        }
    }
}
