plugins {
    java
}

dependencies {
    implementation(project(":skinsrestorer-bukkit", "shadow"))
    implementation(project(":skinsrestorer-bungee", "shadow"))
    implementation(project(":skinsrestorer-velocity", "shadow"))
}

tasks {
    jar {
        archiveFileName = "SkinsRestorer.jar"
        destinationDirectory = rootProject.projectDir.resolve("build/libs")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(configurations.runtimeClasspath)
        from({ configurations.runtimeClasspath.get().map { zipTree(it) } })
    }
}
