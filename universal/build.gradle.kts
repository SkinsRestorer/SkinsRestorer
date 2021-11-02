import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
            val shadowJarTask = platform.tasks.named<ShadowJar>("shadowJar").forUseAtConfigurationTime().get()
            dependsOn(shadowJarTask)
            dependsOn(platform.tasks.withType<Jar>())
            from(zipTree(shadowJarTask.archiveFile))
        }
    }
}
