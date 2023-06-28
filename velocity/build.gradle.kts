plugins {
    alias(libs.plugins.runvelocity)
}

dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared) {
        exclude("net.kyori")
    }

    testImplementation(testFixtures(projects.skinsrestorerShared))

    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.1")

    implementation("net.kyori:adventure-text-serializer-gson:4.14.0") {
        isTransitive = false
    }

    implementation("org.bstats:bstats-velocity:3.0.2")
}

indra {
    javaVersions {
        target(11)
    }
}

tasks {
    runVelocity {
        version(libs.versions.runvelocityversion.get())
    }
    shadowJar {
        relocate("net.kyori.adventure.text.serializer.gson", "net.skinsrestorer.shadow.kyori.normal.gson")
    }
}
