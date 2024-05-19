plugins {
    java
    id("xyz.wagyourtail.jvmdowngrader")
}

dependencies {
    implementation(project(":skinsrestorer-bukkit", "shadow"))
    implementation(project(":skinsrestorer-bungee", "shadow"))
    implementation(project(":skinsrestorer-velocity", "shadow"))
}

jvmdg {
    defaultMavens = false
}

tasks {
    jar {
        archiveFileName = "SkinsRestorer-java17.jar"
        destinationDirectory = rootProject.projectDir.resolve("build/libs")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(configurations.runtimeClasspath)
        from({ configurations.runtimeClasspath.get().map { zipTree(it) } })

        finalizedBy(downgradeJar)
    }
    downgradeJar {
        archiveFileName = "SkinsRestorer.jar"
        destinationDirectory = rootProject.projectDir.resolve("build/libs")

        downgradeTo = JavaVersion.VERSION_1_8
        configureDowngrade {
            jvmArgs = listOf("-Djvmdg.quiet=true")
        }
    }
}
