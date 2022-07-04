plugins {
    id("sr.base-logic")
}

dependencies {
    implementation(projects.skinsrestorerApi)

    compileOnly("org.bukkit:craftbukkit:1.7.10-R0.1-SNAPSHOT")
}
