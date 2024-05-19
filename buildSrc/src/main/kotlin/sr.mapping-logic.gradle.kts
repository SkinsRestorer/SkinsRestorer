plugins {
    java
    id("sr.formatting-logic")
    id("io.github.patrick.remapper")
    id("xyz.wagyourtail.jvmdowngrader")
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
    downgradeJar {
        downgradeTo = JavaVersion.VERSION_17
        archiveClassifier = "downgraded-17"
    }
    remap {
        inputTask = tasks.downgradeJar
        archiveClassifier.set("remapped")
        dependsOn(tasks.downgradeJar)
    }
}

tasks.classes {
    finalizedBy(tasks.downgradeJar)
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
