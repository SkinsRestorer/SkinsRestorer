plugins {
    java
}

dependencies {
    implementation(project(":skinsrestorer-bukkit", "downgraded"))
    implementation(project(":skinsrestorer-bungee", "downgraded"))
    implementation(project(":skinsrestorer-velocity", "downgraded"))
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
