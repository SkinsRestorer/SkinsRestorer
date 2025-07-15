plugins {
    id("sr.base-logic")
    id("xyz.wagyourtail.unimined") version "1.4.2-SNAPSHOT"
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
    modImplementation(libs.adventure.platform.mod.shared.fabric.repack)
    modImplementation(libs.architectury)

    // Mixins
    compileOnly(libs.mixin)

    // Bump fabric api
    fabricModImplementation(enforcedPlatform(libs.fabric.api.bom))
    fabricInclude(enforcedPlatform(libs.fabric.api.bom))

    // Needed for modImplementations to load
    fabricModImplementation(libs.adventure.platform.fabric)
    fabricInclude(libs.adventure.platform.fabric)
    fabricModImplementation(libs.architectury.fabric)
    fabricInclude(libs.architectury.fabric)

    // Fabric source set
    fabricModImplementation(libs.cloud.fabric)
    fabricInclude(libs.cloud.fabric)
    fabricModImplementation(libs.fabric.permissions.api)
    fabricInclude(libs.fabric.permissions.api)

    // Needed for modImplementations to load
    neoforgeModImplementation(libs.adventure.platform.neoforge)
    neoforgeInclude(libs.adventure.platform.neoforge)
    neoforgeModImplementation(libs.architectury.neoforge)
    neoforgeInclude(libs.architectury.neoforge)

    // NeoForge source set
    neoforgeModImplementation(libs.cloud.neoforge)
    neoforgeInclude(libs.cloud.neoforge)
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
