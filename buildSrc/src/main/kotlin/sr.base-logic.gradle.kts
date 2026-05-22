import net.ltgt.gradle.errorprone.errorprone
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
    id("com.github.spotbugs")
    id("org.openrewrite.rewrite")
}

spotbugs {
    ignoreFailures = true
    excludeFilter = file("${rootProject.projectDir}/buildSrc/spotbugs-exclude.xml")
}

rewrite {
    activeRecipe("org.openrewrite.staticanalysis.CommonStaticAnalysis")
    activeRecipe("org.openrewrite.staticanalysis.CodeCleanup")
    activeRecipe("org.openrewrite.staticanalysis.JavaApiBestPractices")
    activeRecipe("org.openrewrite.java.testing.junit5.JUnit5BestPractices")
    activeRecipe("org.openrewrite.java.testing.cleanup.BestPractices")
    activeRecipe("org.openrewrite.java.migrate.UpgradeToJava21")
    isExportDatatables = true
}

dependencies {
    api("org.jetbrains:annotations:26.1.0")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.8")

    errorprone("com.google.errorprone:error_prone_core:2.49.0")
    spotbugs("com.github.spotbugs:spotbugs:4.9.8")

    rewrite("org.openrewrite.recipe:rewrite-static-analysis:2.35.0")
    rewrite("org.openrewrite.recipe:rewrite-migrate-java:3.35.0")
    rewrite("org.openrewrite.recipe:rewrite-rewrite:0.25.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testFixturesApi("org.junit.jupiter:junit-jupiter:6.1.0")
    testFixturesApi("org.mockito:mockito-core:5.23.0")
    testFixturesApi("org.mockito:mockito-junit-jupiter:5.23.0")
}

tasks {
    test {
        useJUnitPlatform()
    }
    // Variable replacements
    processResources {
        // Use inputs.properties to track the expansion properties - this avoids capturing script references
        val localesDir = rootProject.layout.projectDirectory.dir("shared/src/main/resources/locales")
        inputs.property("version", project.version)
        inputs.property("description", project.description ?: "")
        inputs.property("commit", indraGit.commit().map { it.name }.orElse("unknown"))
        inputs.property("branch", indraGit.branchName().orElse("unknown"))
        inputs.property("build_time", SimpleDateFormat("dd MMMM yyyy HH:mm:ss").format(Date()))
        inputs.property(
            "ci_name",
            providers.environmentVariable("GITHUB_ACTIONS").map { if (it == "true") "github-actions" else "local" }
                .orElse(providers.environmentVariable("JENKINS_URL").map { "jenkins" })
                .orElse("local")
        )
        inputs.property(
            "ci_build_number", providers.environmentVariable("BUILD_NUMBER")
                .orElse(providers.environmentVariable("GITHUB_RUN_NUMBER"))
                .orElse("local")
        )
        inputs.property("locales", localesDir.asFile.list()?.joinToString("|") ?: "")

        filesMatching(
            listOf(
                "plugin.yml",
                "bungee.yml",
                "velocity-plugin.json",
                "skinsrestorer-build-data.properties"
            )
        ) {
            expand(inputs.properties.filter {
                it.key in listOf(
                    "version",
                    "description",
                    "commit",
                    "branch",
                    "build_time",
                    "ci_name",
                    "ci_build_number",
                    "locales"
                )
            }
                .plus("url" to "https://skinsrestorer.net"))
        }
    }
    javadoc {
        title = "SkinsRestorer Javadocs"
        options.encoding = Charsets.UTF_8.name()
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }
    compileJava {
        options.errorprone {
            disableWarningsInGeneratedCode = true
        }
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
        languageVersion = JavaLanguageVersion.of(25)
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
