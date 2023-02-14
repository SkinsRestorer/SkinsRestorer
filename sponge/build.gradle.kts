dependencies {
    compileOnly(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.spongepowered:spongeapi:7.4.0")
    annotationProcessor("org.spongepowered:spongeapi:7.4.0")

    implementation("org.bstats:bstats-sponge:3.0.1")
    implementation("com.github.SkinsRestorer.commands:acf-sponge:cd41e746e3")
}
