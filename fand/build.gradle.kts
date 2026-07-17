plugins {
    id("sr.shadow-logic")
}

base {
    archivesName = "SkinsRestorer-Fand"
}

dependencies {
    compileOnly(projects.skinsrestorerShared)
    runtimeOnly(project(":skinsrestorer-shared", "shadow"))

    compileOnly("io.fand:fand-api:${rootProject.property("fandVersion")}")
    testImplementation("io.fand:fand-api:${rootProject.property("fandVersion")}")
    testImplementation(testFixtures(projects.test))
}

tasks.processResources {
    filesMatching("fand-plugin.json") {
        expand(inputs.properties.filter {
            it.key in setOf("version", "description")
        }.plus("url" to "https://skinsrestorer.net"))
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
}
