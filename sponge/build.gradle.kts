dependencies {
    compileOnly(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("org.spongepowered:spongeapi:8.0.0")
    annotationProcessor("org.spongepowered:spongeapi:8.0.0")

    implementation("org.bstats:bstats-sponge:3.0.0")
    implementation("com.github.bloodmc.commands:acf-sponge8:sponge-api8-SNAPSHOT")
}
