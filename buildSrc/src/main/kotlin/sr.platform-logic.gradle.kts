import xyz.wagyourtail.jvmdg.gradle.task.ShadeJar

plugins {
    `java-library`
    id("sr.shadow-logic")
    id("xyz.wagyourtail.jvmdowngrader")
}

tasks {
    val platformDowngrade = register<ShadeJar>("platformDowngrade") {
        dependsOn(tasks.shadowJar)

        inputFile = tasks.shadowJar.get().archiveFile
        downgradeTo = JavaVersion.VERSION_1_8
        archiveFileName.set(
            "SkinsRestorer-${
                project.name.substringAfter("skinsrestorer-").replaceFirstChar(Char::titlecase)
            }-${project.version}-downgraded.jar"
        )

        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
        shadePath = { _ -> "net/skinsrestorer/shadow/jvmdowngrader" }
    }
    shadowJar {
        finalizedBy(platformDowngrade)
    }
}
