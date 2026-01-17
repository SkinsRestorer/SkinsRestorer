import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    java
    id("xyz.wagyourtail.jvmdowngrader")
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
}

dependencies {
    implementation(project(":skinsrestorer-bukkit", "downgraded")) {
        isTransitive = false
    }
    implementation(project(":skinsrestorer-bungee", "downgraded")) {
        isTransitive = false
    }
    implementation(project(":skinsrestorer-velocity", "downgraded")) {
        isTransitive = false
    }
    implementation(projects.multiver.miniplaceholders) {
        isTransitive = false
    }
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
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")

        shadePath = { _ -> "net/skinsrestorer/shadow/jvmdowngrader" }
    }
    build {
        dependsOn(shadeDowngradedApi)
    }
}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version.toString())
        channel.set("Release")
        id.set("SkinsRestorer")
        apiKey.set(providers.environmentVariable("HANGAR_TOKEN"))
        changelog.set(providers.environmentVariable("HANGAR_CHANGELOG"))
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
