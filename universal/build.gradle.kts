plugins {
    java
    id("xyz.wagyourtail.jvmdowngrader")
}

dependencies {
    implementation(project(":skinsrestorer-bukkit", "downgraded"))
    implementation(project(":skinsrestorer-bungee", "downgraded"))
    implementation(project(":skinsrestorer-velocity", "downgraded"))
}

tasks {
    jar {
        archiveClassifier = "only-merged"

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(configurations.runtimeClasspath)
        from({ configurations.runtimeClasspath.get().map { zipTree(it) } })
    }
    shadeDowngradedApi {
        dependsOn(jar)

        inputFile = jar.get().archiveFile
        downgradeTo = JavaVersion.VERSION_1_8

        archiveFileName = "SkinsRestorer.jar"
        destinationDirectory = rootProject.projectDir.resolve("build/libs")

        shadePath = { _ -> "net/skinsrestorer/shadow/jvmdowngrader" }
    }
    build {
        dependsOn(shadeDowngradedApi)
    }
}
