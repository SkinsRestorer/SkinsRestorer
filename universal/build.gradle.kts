plugins {
    id("sr.base-logic")
    id("xyz.wagyourtail.jvmdowngrader")
}

dependencies {
    implementation(project(":skinsrestorer-bukkit", "shadow"))
    implementation(project(":skinsrestorer-bungee", "shadow"))
    implementation(project(":skinsrestorer-velocity", "shadow"))
}

tasks {
    jar {
        archiveFileName = "SkinsRestorer-java17.jar"
        destinationDirectory = rootProject.projectDir.resolve("build/libs")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith("jar") }
                .filter { it.toString().contains("build/libs") }
                .map { zipTree(it) }
        })

        finalizedBy(downgradeJar)
    }
    downgradeJar {
        downgradeTo = JavaVersion.VERSION_1_8
        destinationDirectory = rootProject.projectDir.resolve("build/libs")
        archiveFileName = "SkinsRestorer.jar"
    }
}
