import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeJar

plugins {
    `java-library`
    id("xyz.wagyourtail.jvmdowngrader")
    id("sr.shadow-logic")
}

val downgradePlatformBase = tasks.register<DowngradeJar>("downgradePlatformBase") {
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    downgradeTo = JavaVersion.VERSION_1_8
    archiveClassifier.set("downgraded-base")
}

val downgradePlatformShadow = tasks.register<ShadeJar>("downgradePlatformShadow") {
    inputFile.set(downgradePlatformBase.flatMap { it.archiveFile })
    downgradeTo = JavaVersion.VERSION_1_8
    archiveFileName.set(base.archivesName.map { "$it-${project.version}-downgraded.jar" })

    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
    shadePath = { _ -> "net/skinsrestorer/shadow/jvmdowngrader" }
}

tasks {
    val downgradedTest by tasks.registering(Test::class) {
        group = "verification"
        useJUnitPlatform()
        classpath =
            files(downgradePlatformShadow.flatMap { it.archiveFile }) + sourceSets.test.get().output + sourceSets.test.get().runtimeClasspath - sourceSets.main.get().output
    }
    check {
        dependsOn(downgradedTest)
    }
}

configurations.create("downgraded")

artifacts {
    add("downgraded", downgradePlatformBase.flatMap { it.archiveFile }) {
        builtBy(downgradePlatformBase)
    }
}
