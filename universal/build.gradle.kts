import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.raphimc.javadowngrader.gradle.task.DowngradeJarTask

plugins {
    id("net.raphimc.java-downgrader") version "1.1.2-SNAPSHOT"
}

val platforms = setOf(
    rootProject.projects.skinsrestorerBukkit,
    rootProject.projects.skinsrestorerBungee,
    rootProject.projects.skinsrestorerSponge,
    rootProject.projects.skinsrestorerVelocity
).map { it.dependencyProject }

tasks {
    jar {
        archiveClassifier.set("")
        archiveFileName.set("SkinsRestorer.jar")
        destinationDirectory.set(rootProject.projectDir.resolve("build/libs"))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        platforms.forEach { platform ->
            val shadowJarTask = platform.tasks.named<ShadowJar>("shadowJar").get()
            dependsOn(shadowJarTask)
            dependsOn(platform.tasks.withType<Jar>())
            from(zipTree(shadowJarTask.archiveFile))
        }
        finalizedBy("java8Jar")
    }
    register<DowngradeJarTask>("java8Jar") {
        input.set(jar.get().archiveFile.get().asFile)
    }.get().dependsOn("jar")
}
