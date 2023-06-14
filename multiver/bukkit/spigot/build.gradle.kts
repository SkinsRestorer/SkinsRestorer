plugins {
    id("sr.base-logic")
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
}
