plugins {
    java
    id("sr.formatting-logic")
    id("sr.core-dependencies")
    id("io.github.patrick.remapper")
}

dependencies {
    implementation(project(":multiver:bukkit:shared"))
}

tasks.remap {
    archiveClassifier.set("remapped")
    dependsOn(tasks.jar)
}

tasks.build {
    dependsOn(tasks.remap)
}

configurations {
    create("remapped") {
        val resultFile = File(
            File(project.layout.buildDirectory.asFile.get(), "libs"),
            "${project.name}-${project.version}-remapped.jar"
        )
        val files = project.files(resultFile)
        files.builtBy("remap")

        isCanBeResolved = false
        isCanBeConsumed = true
        outgoing.artifact(resultFile)
        dependencies.add(project.dependencies.create(files))
    }
}
