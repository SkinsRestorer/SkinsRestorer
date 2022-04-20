dependencies {
    compileOnly(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    compileOnly("com.velocitypowered:velocity-api:3.0.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.0.1")

    implementation("org.bstats:bstats-velocity:3.0.0")
    implementation("co.aikar:acf-velocity:0.5.1-SNAPSHOT")
}
