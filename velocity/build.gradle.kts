dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.1")

    implementation("org.bstats:bstats-velocity:3.0.1")
    implementation("com.github.SkinsRestorer.commands:acf-velocity:cd41e746e3")
}
