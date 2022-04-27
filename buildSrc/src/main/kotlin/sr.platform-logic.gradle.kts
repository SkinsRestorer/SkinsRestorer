import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("sr.shadow-logic")
}

(tasks.getByName("shadowJar") as ShadowJar).archiveFileName.set(
    "SkinsRestorer-${
        project.name.substringAfter("skinsrestorer-").capitalize()
    }-${project.version}.jar"
)

(tasks.getByName("shadowJar") as ShadowJar).destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
