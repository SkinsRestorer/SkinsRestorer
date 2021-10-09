plugins {
    `java-library`
    `maven-publish`
    id("sr.license-logic")
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

dependencies.compileOnly("org.projectlombok:lombok:1.18.22")
dependencies.annotationProcessor("org.projectlombok:lombok:1.18.22")
dependencies.implementation("org.jetbrains:annotations:22.0.0")

java.javaTarget(8)
java.withSourcesJar()

publishing.publications.create<MavenPublication>("mavenJava") {
    groupId = rootProject.group as String
    artifactId = project.name
    version = rootProject.version as String
}

val repoName = if (version.toString().endsWith("SNAPSHOT")) "maven-snapshots" else "maven-releases"
publishing.repositories.maven("https://repo.codemc.org/repository/${repoName}/") {
    credentials.username = System.getenv("CODEMC_USERNAME")
    credentials.password = System.getenv("CODEMC_PASSWORD")
    name = "codemc"
}
