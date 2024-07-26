
plugins {
    java
    id("sr.formatting-logic")
    id("io.freefair.lombok")
    id("xyz.wagyourtail.unimined") version "1.3.3"
}

val main: SourceSet by sourceSets.getting
val fabric: SourceSet by sourceSets.creating
val neoforge: SourceSet by sourceSets.creating

unimined.minecraft {
    version = "1.21"

    mappings {
        intermediary()
        mojmap()

        devFallbackNamespace("official")
    }

    if (sourceSet == main) {
        mods {
            modImplementation {
                namespace("intermediary")
            }
        }
        runs {
            off = true
        }
    }

    defaultRemapJar = false
}

unimined.minecraft(fabric) {
    combineWith(main)

    fabric {
        loader("0.15.11")
    }

    defaultRemapJar = true
}

unimined.minecraft(neoforge) {
    combineWith(main)

    neoForge {
        loader("0-beta")
    }

    minecraftRemapper.config {
        // neoforge adds 1 conflict, where 2 interfaces have a method with the same name on yarn/mojmap,
        // but the method has different names in the intermediary mappings.
        // this is a conflict because they have a class that extends both interfaces.
        // this shouldn't be a problem as long as named mappings don't make the name of those 2 methods different.
        ignoreConflicts(true)
    }

    defaultRemapJar = true
}

val modImplementation: Configuration by configurations.getting
val fabricModImplementation: Configuration by configurations.getting
val neoforgeModImplementation: Configuration by configurations.getting
val fabricImplementation: Configuration by configurations.getting
val neoforgeImplementation: Configuration by configurations.getting

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    fabricImplementation(projects.skinsrestorerApi)
    fabricImplementation(projects.skinsrestorerShared)
    neoforgeImplementation(projects.skinsrestorerApi)
    neoforgeImplementation(projects.skinsrestorerShared)

    implementation("net.lenni0451.mcstructs:text:2.5.1")
    implementation("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    modImplementation("dev.architectury:architectury:13.0.6")

    fabricModImplementation(fabricApi.fabric("0.100.7+1.21"))
    fabricModImplementation("dev.architectury:architectury-fabric:13.0.6")
    val cloudFabric = "org.incendo:cloud-fabric:2.0.0-SNAPSHOT"
    fabricModImplementation(cloudFabric)

    neoforgeModImplementation("dev.architectury:architectury-neoforge:13.0.6")
    val cloudNeoForge = "org.incendo:cloud-neoforge:2.0.0-SNAPSHOT"
    neoforgeModImplementation(cloudNeoForge)
}

tasks.getByName<ProcessResources>("processFabricResources") {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.getByName<ProcessResources>("processNeoforgeResources") {
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}
