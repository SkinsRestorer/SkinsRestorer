import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("sr.shadow-logic")
}

(tasks.getByName("shadowJar") as ShadowJar).archiveFileName.set(
    "SkinsRestorer-${
        project.name.substringAfter("skinsrestorer-").replaceFirstChar(Char::titlecase)
    }-${project.version}.jar"
)

(tasks.getByName("shadowJar") as ShadowJar).destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
