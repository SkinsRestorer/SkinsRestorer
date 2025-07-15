plugins {
    id("sr.base-logic")
}

dependencies {
    compileOnly(projects.skinsrestorerShared)

    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.miniplaceholders.api)
}
