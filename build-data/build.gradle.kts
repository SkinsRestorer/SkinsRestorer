plugins {
    id("sr.formatting-logic")
    id("net.kyori.blossom")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

blossom {
    replaceToken("{version}", version)
    replaceToken("{description}", rootProject.description)
    replaceToken("{url}", "https://skinsrestorer.net")
    replaceToken("{commit}", rootProject.latestCommitHash())

    val sharedResources = rootDir.resolve("shared").resolve("src").resolve("main").resolve("resources")
    replaceToken("{locales}", sharedResources.resolve("languages").list().joinToString("|"))
}
