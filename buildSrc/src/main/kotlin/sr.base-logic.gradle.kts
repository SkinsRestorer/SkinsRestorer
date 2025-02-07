plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
    id("sr.formatting-logic")
    id("net.kyori.indra.git")
    id("io.freefair.lombok")
}

dependencies {
    api("org.jetbrains:annotations:26.0.2")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testFixturesApi("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testFixturesApi("org.mockito:mockito-core:5.15.2")
    testFixturesApi("org.mockito:mockito-junit-jupiter:5.15.2")
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

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "SkinsRestorer"
                description = rootProject.description
                url = "https://skinsrestorer.net"
                organization {
                    name = "SkinsRestorer"
                    url = "https://skinsrestorer.net"
                }
                developers {
                    developer {
                        id = "xknat"
                        timezone = "Europe/Amsterdam"
                        url = "https://github.com/xknat"
                    }
                    developer {
                        id = "AlexProgrammerDE"
                        timezone = "Europe/Berlin"
                        url = "https://pistonmaster.net"
                    }
                }
                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.html"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/SkinsRestorer/SkinsRestorer.git"
                    developerConnection = "scm:git:ssh://git@github.com/SkinsRestorer/SkinsRestorer.git"
                    url = "https://github.com/SkinsRestorer/SkinsRestorer"
                }
                ciManagement {
                    system = "GitHub Actions"
                    url = "https://github.com/SkinsRestorer/SkinsRestorer/actions"
                }
                issueManagement {
                    system = "GitHub"
                    url = "https://github.com/SkinsRestorer/SkinsRestorer/issues"
                }
            }
        }
    }
}
