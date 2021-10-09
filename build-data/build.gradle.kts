plugins {
    id("net.kyori.blossom")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

blossom {
    tokenReplacementsGlobalLocations.clear();
    replaceTokenIn("src/main/java/net/skinsrestorer/api/builddata/BuildData.java")

    replaceToken("{version}", version)
    replaceToken("{description}", rootProject.description)
    replaceToken("{url}", "https://skinsrestorer.net/")
}