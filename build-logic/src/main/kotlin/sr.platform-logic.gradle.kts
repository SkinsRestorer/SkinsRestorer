import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("sr.shadow-logic")
}

dependencies.implementation("com.google.code.gson:gson:2.8.9")
val spiget: ExternalModuleDependency = dependencies.implementation("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") as ExternalModuleDependency
spiget.exclude("org.bukkit", "bukkit")

(tasks.getByName("shadowJar") as ShadowJar).archiveFileName.set(
    "SkinsRestorer-${
        project.name.substringAfter("skinsrestorer-").capitalize()
    }-${project.version}.jar"
)

(tasks.getByName("shadowJar") as ShadowJar).destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
