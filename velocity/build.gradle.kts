dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    testImplementation(testFixtures(projects.skinsrestorerShared))

    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.1")

    implementation("org.bstats:bstats-velocity:3.0.2")
}

indra {
    javaVersions {
        target(11)
    }
}
