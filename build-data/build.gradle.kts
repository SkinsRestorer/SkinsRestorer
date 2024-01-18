import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("sr.formatting-logic")
    alias(libs.plugins.blossom)
    id("sr.core-dependencies")
    id("net.kyori.indra.git")
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", version.toString())
                property("description", rootProject.description)
                property("url", "https://skinsrestorer.net")
                property("commit", indraGit.commit()?.name ?: "unknown")
                property("branch", indraGit.branch()?.name ?: "unknown")
                property("build_time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
                property("ci_name", getCIName())
                property("ci_build_number", getBuildNumber())

                val sharedResources = rootDir.resolve("shared").resolve("src").resolve("main").resolve("resources")
                property("locales", sharedResources.resolve("locales").list()?.joinToString("|"))
            }
        }
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
