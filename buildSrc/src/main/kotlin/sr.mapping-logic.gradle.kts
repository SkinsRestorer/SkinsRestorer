import net.raphimc.javadowngrader.gradle.task.DowngradeSourceSetTask

plugins {
    java
    id("sr.formatting-logic")
    id("sr.core-dependencies")
    id("io.github.patrick.remapper")
    id("net.raphimc.java-downgrader")
}

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
    remap {
        archiveClassifier.set("remapped")
        dependsOn(tasks.jar)
    }
    build {
        dependsOn(tasks.remap)
    }
}

val java17Remap = tasks.register<DowngradeSourceSetTask>("java17Remap") {
    targetVersion = 17
    sourceSet = sourceSets.main
}.get().dependsOn(tasks.classes)
tasks.classes {
    finalizedBy(java17Remap)
}

@Suppress("UnstableApiUsage")
configurations {
    create("remapped") {
        val resultFile = File(
            File(project.layout.buildDirectory.asFile.get(), "libs"),
            "${project.name}-${project.version}-remapped.jar"
        )
        val files = project.files(resultFile)
        files.builtBy(tasks.remap)

        isCanBeResolved = false
        isCanBeConsumed = true
        outgoing.artifact(resultFile)
        dependencies.add(project.dependencies.create(files))

        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
}
