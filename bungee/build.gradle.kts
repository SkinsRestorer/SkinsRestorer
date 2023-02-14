plugins {
    id("sr.base-logic")
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    implementation(projects.multiver.bungee.shared)
    implementation(projects.multiver.bungee.propertyold)
    implementation(projects.multiver.bungee.propertynew)

    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-proxy:1.19-R0.1-SNAPSHOT")

    implementation("org.bstats:bstats-bungeecord:3.0.1")
    implementation("com.github.SkinsRestorer.commands:acf-bungee:cd41e746e3")
}
