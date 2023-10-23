plugins {
    id("sr.formatting-logic")
    id("net.kyori.blossom")
    id("sr.core-dependencies")
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
                property("commit", rootProject.latestCommitHash())

                val sharedResources = rootDir.resolve("shared").resolve("src").resolve("main").resolve("resources")
                property("locales", sharedResources.resolve("locales").list()?.joinToString("|"))
            }
        }
    }
}
