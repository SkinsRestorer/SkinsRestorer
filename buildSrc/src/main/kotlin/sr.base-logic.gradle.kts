plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
    signing
    id("sr.formatting-logic")
    id("net.kyori.indra")
    id("net.kyori.indra.git")
    id("io.freefair.lombok")
}

dependencies {
    api("org.jetbrains:annotations:26.0.1")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testFixturesApi("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testFixturesApi("org.mockito:mockito-core:5.14.2")
    testFixturesApi("org.mockito:mockito-junit-jupiter:5.14.2")
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
        target(21)
        strictVersions()
        testWith(21)
        minimumToolchain(21)
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
