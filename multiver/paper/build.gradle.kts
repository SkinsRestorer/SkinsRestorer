plugins {
    id("sr.base-logic")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("io.papermc.paper:paper-api:1.19-R0.1-SNAPSHOT")
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
