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
        archiveFileName.set("SkinsRestorer-java17.jar")
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
    val java8Jar = register<DowngradeJarTask>("java8Jar") {
        input.set(jar.get().archiveFile.get().asFile)
        dependsOn(jar)
        finalizedBy("fixJava8FileName")
    }
    register<Copy>("fixJava8FileName") {
        val outputFolder = rootProject.projectDir.resolve("build/libs")
        val inputName = "SkinsRestorer-java17-downgraded.jar"
        val outputName = "SkinsRestorer.jar"
        from(outputFolder)
        include(inputName)
        destinationDir = outputFolder
        rename(inputName, outputName)
        dependsOn(java8Jar)
    }
}
