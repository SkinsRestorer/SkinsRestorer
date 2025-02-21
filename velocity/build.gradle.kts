plugins {
    id("sr.platform-logic")
    alias(libs.plugins.runvelocity)
}

dependencies {
    compileOnly(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared) {
        exclude("net.kyori")
    }

    testImplementation(testFixtures(projects.skinsrestorerShared))

    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.1")

    implementation("org.bstats:bstats-velocity:3.1.0")
    implementation("org.incendo:cloud-velocity:2.0.0-SNAPSHOT")
}

tasks {
    runVelocity {
        version(libs.versions.runvelocityversion.get())
    }
}
