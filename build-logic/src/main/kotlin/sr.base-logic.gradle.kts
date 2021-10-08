plugins {
    `java-library`
    `maven-publish`
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
        options.compilerArgs.addAll(listOf("-parameters", "-nowarn", "-Xlint:-unchecked", "-Xlint:-deprecation", "-Xlint:-processing"))
        options.isFork = true
    }
}

java {
    javaTarget(8)
    withSourcesJar()
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        groupId = rootProject.group as String
        artifactId = project.name
        version = rootProject.version as String
    }
    repositories {
        maven {
            credentials {
                username = System.getenv("CODEMC_USERNAME")
                password = System.getenv("CODEMC_PASSWORD")
            }
            name = "codemc"
            val repoName = if (version.toString().endsWith("SNAPSHOT")) "maven-snapshots" else "maven-releases"
            url = uri("https://repo.codemc.org/repository/${repoName}/")
        }
    }
}