dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared) {
        exclude("net.kyori")
    }

    testImplementation(testFixtures(projects.skinsrestorerShared))

    implementation("org.bstats:bstats-sponge:3.0.2")
    compileOnly("com.mojang:authlib:2.0.27")

    compileOnly("org.spongepowered:spongeapi:8.2.0")
}

indra {
    javaVersions {
        target(17)
    }
}
