dependencies {
    compileOnly(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.spongepowered:spongeapi:9.0.0")
    annotationProcessor("org.spongepowered:spongeapi:9.0.0")

    implementation("org.bstats:bstats-sponge:3.0.0")
    implementation("co.aikar:acf-sponge:0.5.1-SNAPSHOT")
}
