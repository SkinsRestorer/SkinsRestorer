import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    java
    id("xyz.wagyourtail.jvmdowngrader")
    id("io.papermc.hangar-publish-plugin") version "0.1.3"
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

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set("Release")
        id.set("SkinsRestorer")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms {
            register(Platforms.PAPER) {
                jar.set(tasks.shadeDowngradedApi.flatMap { it.archiveFile })

                val versions: List<String> = (property("paperVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
            register(Platforms.VELOCITY) {
                jar.set(tasks.shadeDowngradedApi.flatMap { it.archiveFile })

                val versions: List<String> = (property("velocityVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
            register(Platforms.WATERFALL) {
                jar.set(tasks.shadeDowngradedApi.flatMap { it.archiveFile })

                val versions: List<String> = (property("waterfallVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
        }
    }
}
