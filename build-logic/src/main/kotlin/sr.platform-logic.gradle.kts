plugins {
    id("sr.shadow-logic")
}

tasks {
    shadowJar {
        archiveFileName.set("SkinsRestorer-${project.name.substringAfter("skinsrestorer-").capitalize()}-${project.version}.jar")
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
    }
}