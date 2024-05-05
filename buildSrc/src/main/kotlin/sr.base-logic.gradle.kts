plugins {
    `java-library`
    `maven-publish`
    signing
    id("sr.formatting-logic")
    id("sr.core-dependencies")
    id("net.kyori.indra")
    id("net.kyori.indra.git")
}

tasks {
    // Variable replacements
    processResources {
        filesMatching(listOf("plugin.yml", "bungee.yml")) {
            expand("version" to project.version, "description" to project.description)
        }
    }
    javadoc {
        title = "SkinsRestorer Javadocs"
        options.encoding = Charsets.UTF_8.name()
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
        onlyIf { project.name.contains("api") }
    }
    delombok {
        onlyIf { project.name.contains("api") }
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "-nowarn",
                "-Xlint:-unchecked",
                "-Xlint:-deprecation",
                "-Xlint:-processing"
            )
        )
        options.isFork = true
    }
}

indra {
    github("SkinsRestorer", "SkinsRestorer") {
        ci(true)
    }

    gpl3OnlyLicense()
    publishReleasesTo("codemc-releases", "https://repo.codemc.org/repository/maven-releases/")
    publishSnapshotsTo("codemc-snapshots", "https://repo.codemc.org/repository/maven-snapshots/")

    configurePublications {
        pom {
            name.set("SkinsRestorer")
            url.set("https://skinsrestorer.net/")
            organization {
                name.set("SkinsRestorer")
                url.set("https://skinsrestorer.net")
            }
            developers {
                developer {
                    id.set("xknat")
                    timezone.set("Europe/Amsterdam")
                    url.set("https://github.com/xknat")
                }
                developer {
                    id.set("AlexProgrammerDE")
                    timezone.set("Europe/Berlin")
                    url.set("https://pistonmaster.net")
                }
            }
        }

        versionMapping {
            usage(Usage.JAVA_API) { fromResolutionOf(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) }
            usage(Usage.JAVA_RUNTIME) { fromResolutionResult() }
        }
    }

    javaVersions {
        target(17)
        strictVersions()
        testWith(17)
        minimumToolchain(17)
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf { false }
}

tasks {
    all {
        if (name == "testJava8") {
            onlyIf { false }
        }
    }
}
