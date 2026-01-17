import com.github.jengelman.gradle.plugins.shadow.transformers.PreserveFirstFoundResourceTransformer

plugins {
    id("sr.platform-logic")
    alias(libs.plugins.runwaterfall)
}

base {
    archivesName = "SkinsRestorer-Bungee"
}

dependencies {
    compileOnly(projects.skinsrestorerShared)
    runtimeOnly(project(":skinsrestorer-shared", "shadow"))
    testImplementation(testFixtures(projects.test))

    compileOnly(libs.bungeecord.api)

    implementation(libs.bstats.bungeecord)
    implementation(libs.cloud.bungee)
}

tasks {
    runWaterfall {
        version(libs.versions.runwaterfallversion.get())
    }
}

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
        filesNotMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
        transform<PreserveFirstFoundResourceTransformer>()
        exclude("META-INF/annotations.shadow.kotlin_module")
        relocate("net.kyori", "net.skinsrestorer.shadow.kyori")
    }
}
