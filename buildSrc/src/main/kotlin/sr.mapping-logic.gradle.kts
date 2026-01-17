plugins {
    java
    id("sr.formatting-logic")
    id("xyz.wagyourtail.jvmdowngrader")
}

// Create extension first, before applying the plugin
val mapping = extensions.create("mapping", MappingExtension::class.java)

plugins.apply(MappingPlugin::class.java)

dependencies {
    implementation(project(":multiver:bukkit:shared"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    downgradeJar {
        downgradeTo = JavaVersion.VERSION_17
        archiveClassifier = "downgraded-17"
    }
}

tasks.classes {
    finalizedBy(tasks.downgradeJar)
}

val remap = tasks.register<SpigotRemapTask>("remap") {
    val mcVersion = mapping.mcVersion

    projectName.set(project.name)
    version.set(mcVersion)
    inputFile.set(tasks.downgradeJar.flatMap { it.archiveFile })
    archiveClassifier.set("remapped")

    val action = SpigotRemapTask.RemapAction.MOJANG_TO_SPIGOT
    this.action.set(action)

    // Configure mapping and inheritance files lazily
    action.procedures.forEach { procedure ->
        mappingFiles.from(
            mcVersion.map { v ->
                configurations.detachedConfiguration(
                    dependencies.create(procedure.mappingCoord(v))
                ).apply {
                    isTransitive = false
                }
            }
        )
        inheritanceFiles.from(
            mcVersion.map { v ->
                configurations.detachedConfiguration(
                    dependencies.create(procedure.inheritanceCoord(v))
                ).apply {
                    isTransitive = false
                }
            }
        )
    }
}

configurations {
    create("remapped") {
        isCanBeResolved = false
        isCanBeConsumed = true
        outgoing.artifact(remap.flatMap { it.outputFile }) {
            builtBy(remap)
        }

        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
}
