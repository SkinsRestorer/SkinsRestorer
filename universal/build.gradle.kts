import net.raphimc.javadowngrader.gradle.task.DowngradeJarTask

plugins {
    id("sr.base-logic")
    id("net.raphimc.java-downgrader")
}

dependencies {
    implementation(project(":skinsrestorer-bukkit", "shadow"))
    implementation(project(":skinsrestorer-bungee", "shadow"))
    implementation(project(":skinsrestorer-velocity", "shadow"))
}

tasks {
    jar {
        archiveClassifier.set("")
        archiveFileName.set("SkinsRestorer-java17.jar")
        destinationDirectory.set(rootProject.projectDir.resolve("build/libs"))

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith("jar") }
                .filter { it.toString().contains("build/libs") }
                .map { zipTree(it) }
        })

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
