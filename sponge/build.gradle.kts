dependencies {
    compileOnly(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.spongepowered:spongeapi:8.0.0")
    annotationProcessor("org.spongepowered:spongeapi:8.0.0")

    implementation("org.bstats:bstats-sponge:2.2.1")
    implementation("co.aikar:acf-sponge:0.5.0-SNAPSHOT")
}
