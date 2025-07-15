plugins {
    id("sr.base-logic")
}

dependencies {
    compileOnly(projects.skinsrestorerShared)
    runtimeOnly(project(":skinsrestorer-shared", "shadow"))
    api(projects.multiver.viaversion)

    compileOnly(libs.spigot.api) {
        isTransitive = false
    }
}
