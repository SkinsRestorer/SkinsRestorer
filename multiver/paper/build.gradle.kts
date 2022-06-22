plugins {
    id("sr.base-logic")
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
}

configurations {
    create("multiverbuild") {
        val resultFile = File(File(project.buildDir, "libs"), "${project.name}-${project.version}.jar")
        val files = project.files(resultFile)
        files.builtBy("jar")

        isCanBeResolved = false
        isCanBeConsumed = true
        outgoing.artifact(resultFile)
        dependencies.add(project.dependencies.create(files))
    }
}
