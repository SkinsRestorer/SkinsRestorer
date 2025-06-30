plugins {
    id("sr.base-logic")
    id("xyz.wagyourtail.unimined") version "1.3.15"
}

base {
    archivesName = "SkinsRestorer-Mod"
}

val main: SourceSet by sourceSets.getting
val fabric: SourceSet by sourceSets.creating
val neoforge: SourceSet by sourceSets.creating

unimined.minecraft {
    version = property("modMcVersion") as String

    mappings {
        intermediary()
        mojmap()
        parchment("1.21.6", "2025.06.29")

        devFallbackNamespace("official")
    }

    accessWidener {
        accessWidener(project.projectDir.resolve("src/main/resources/skinsrestorer.accesswidener"))
    }

    if (sourceSet == main) {
        mods {
            modImplementation {
                namespace("intermediary")
            }
        }
        runs.off = true
        defaultRemapJar = false
    } else {
        runs {
            config("server") {
                standardInput = System.`in`
            }
        }
    }
}

unimined.minecraft(fabric) {
    combineWith(main)

    fabric {
        loader("0.16.14")
        accessWidener(project.projectDir.resolve("src/main/resources/skinsrestorer.accesswidener"))
    }

    mods.modImplementation {
        mixinRemap {
            @Suppress("UnstableApiUsage")
            reset()
            enableBaseMixin()
            enableMixinExtra()
        }
    }
}

unimined.minecraft(neoforge) {
    combineWith(main)

    neoForge {
        loader("1-beta")
        accessTransformer(aw2at(project.projectDir.resolve("src/main/resources/skinsrestorer.accesswidener")))
    }

    @Suppress("UnstableApiUsage")
    minecraftRemapper.config {
        // neoforge adds 1 conflict, where 2 interfaces have a method with the same name on yarn/mojmap,
        // but the method has different names in the intermediary mappings.
        // this is a conflict because they have a class that extends both interfaces.
        // this shouldn't be a problem as long as named mappings don't make the name of those 2 methods different.
        ignoreConflicts(true)
    }

    mods.modImplementation {
        mixinRemap {
            @Suppress("UnstableApiUsage")
            reset()
            enableBaseMixin()
            enableMixinExtra()
        }
    }
}

val modImplementation: Configuration by configurations.getting
val fabricModImplementation: Configuration by configurations.getting
val neoforgeModImplementation: Configuration by configurations.getting
val fabricInclude: Configuration by configurations.getting
val neoforgeInclude: Configuration by configurations.getting
val fabricImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
val neoforgeImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    setOf(
        projects.skinsrestorerShared,
        projects.multiver.miniplaceholders,
        projects.multiver.viaversion
    ).forEach {
        implementation(it) {
            exclude("com.google.code.gson")
            exclude("com.google.errorprone")
        }
        fabricInclude(it) {
            exclude("com.google.code.gson")
            exclude("com.google.errorprone")
        }
        neoforgeInclude(it) {
            exclude("com.google.code.gson")
            exclude("com.google.errorprone")
        }
    }

    // Shared mods
    modImplementation("net.kyori:adventure-platform-mod-shared-fabric-repack:6.5.0-SNAPSHOT")
    modImplementation("dev.architectury:architectury:17.0.6")

    // Mixins
    compileOnly("org.spongepowered:mixin:0.8.7")

    // Needed for modImplementations to load
    fabricModImplementation("net.kyori:adventure-platform-fabric:6.5.0-SNAPSHOT")
    fabricInclude("net.kyori:adventure-platform-fabric:6.5.0-SNAPSHOT")
    fabricModImplementation("dev.architectury:architectury-fabric:17.0.6")
    fabricInclude("dev.architectury:architectury-fabric:17.0.6")

    // Fabric source set
    fabricModImplementation("org.incendo:cloud-fabric:2.0.0-SNAPSHOT")
    fabricInclude("org.incendo:cloud-fabric:2.0.0-SNAPSHOT")
    fabricModImplementation("me.lucko:fabric-permissions-api:0.4.1")
    fabricInclude("me.lucko:fabric-permissions-api:0.4.1")

    // Needed for modImplementations to load
    neoforgeModImplementation("net.kyori:adventure-platform-neoforge:6.5.0-SNAPSHOT")
    neoforgeInclude("net.kyori:adventure-platform-neoforge:6.5.0-SNAPSHOT")
    neoforgeModImplementation("dev.architectury:architectury-neoforge:17.0.6")
    neoforgeInclude("dev.architectury:architectury-neoforge:17.0.6")

    // NeoForge source set
    neoforgeModImplementation("org.incendo:cloud-neoforge:2.0.0-SNAPSHOT")
    neoforgeInclude("org.incendo:cloud-neoforge:2.0.0-SNAPSHOT")
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
