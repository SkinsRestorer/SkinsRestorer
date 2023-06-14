plugins {
    `java-library`
    id("sr.shadow-logic")
}

tasks.shadowJar {
    archiveFileName.set(
        "SkinsRestorer-${
            project.name.substringAfter("skinsrestorer-").replaceFirstChar(kotlin.Char::titlecase)
        }-${project.version}.jar"
    )

    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
}
