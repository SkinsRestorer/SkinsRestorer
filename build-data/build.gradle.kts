import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("sr.formatting-logic")
    alias(libs.plugins.blossom)
    id("sr.core-dependencies")
    id("net.kyori.indra.git")
}

java {
    javaTarget(8)
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
                property("ci_name", System.getenv("CI_NAME") ?: "local")
                property("ci_build_number", System.getenv("CI_BUILD_NUMBER") ?: "local")

                val sharedResources = rootDir.resolve("shared").resolve("src").resolve("main").resolve("resources")
                property("locales", sharedResources.resolve("locales").list()?.joinToString("|"))
            }
        }
    }
}
