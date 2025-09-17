import java.text.SimpleDateFormat
import java.util.*

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
    id("sr.formatting-logic")
    id("net.kyori.indra.git")
    id("io.freefair.lombok")
    id("net.ltgt.errorprone")
}

dependencies {
    api("org.jetbrains:annotations:26.0.2-1")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.6")

    errorprone("com.google.errorprone:error_prone_core:2.41.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testFixturesApi("org.junit.jupiter:junit-jupiter:5.13.4")
    testFixturesApi("org.mockito:mockito-core:5.19.0")
    testFixturesApi("org.mockito:mockito-junit-jupiter:5.19.0")
}

tasks {
    test {
        useJUnitPlatform()
    }
    // Variable replacements
    processResources {
        filesMatching(
            listOf(
                "plugin.yml",
                "bungee.yml",
                "velocity-plugin.json",
                "skinsrestorer-build-data.properties"
            )
        ) {
            val sharedResources = rootDir.resolve("shared").resolve("src").resolve("main").resolve("resources")
            expand(
                mapOf(
                    "version" to project.version,
                    "description" to project.description,
                    "url" to "https://skinsrestorer.net",
                    "commit" to (indraGit.commit()?.name ?: "unknown"),
                    "branch" to (indraGit.branch()?.name ?: "unknown"),
                    "build_time" to SimpleDateFormat("dd MMMM yyyy HH:mm:ss").format(Date()),
                    "ci_name" to getCIName(),
                    "ci_build_number" to getBuildNumber(),
                    "locales" to sharedResources.resolve("locales").list()?.joinToString("|")
                )
            )
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

fun getCIName(): String {
    val githubActions = System.getenv("GITHUB_ACTIONS")
    val jenkinsUrl = System.getenv("JENKINS_URL")
    if (githubActions != null && githubActions == "true") {
        return "github-actions"
    } else if (jenkinsUrl != null) {
        return "jenkins"
    }

    return "local"
}

fun getBuildNumber(): String {
    return System.getenv("BUILD_NUMBER") ?: System.getenv("GITHUB_RUN_NUMBER") ?: "local"
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
