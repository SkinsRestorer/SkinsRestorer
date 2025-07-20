plugins {
    id("sr.platform-logic")
    alias(libs.plugins.runvelocity)
}

base {
    archivesName = "SkinsRestorer-Velocity"
}

dependencies {
    compileOnly(projects.skinsrestorerShared)
    runtimeOnly(project(":skinsrestorer-shared", "shadow"))
    implementation(projects.multiver.miniplaceholders)

    testImplementation(testFixtures(projects.test))

    compileOnly(libs.velocity.api)

    implementation(libs.bstats.velocity)
    implementation(libs.cloud.velocity)
}

tasks {
    runVelocity {
        version(libs.versions.runvelocityversion.get())
    }
}
