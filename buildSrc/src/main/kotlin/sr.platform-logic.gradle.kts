import xyz.wagyourtail.jvmdg.gradle.task.ShadeJar

plugins {
    `java-library`
    id("xyz.wagyourtail.jvmdowngrader")
    id("sr.shadow-logic")
}

val downgradePlatform = tasks.register<ShadeJar>("downgradePlatform") {
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

configurations.create("downgraded")

artifacts {
    add("downgraded", downgradePlatform.get().archiveFile) {
        builtBy(downgradePlatform)
    }
}
