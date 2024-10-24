plugins {
    id("sr.shadow-logic")
}

dependencies {
    implementation(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)

    api("com.google.code.gson:gson:2.11.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1") {
        exclude("com.github.waffle", "waffle-jna")
    }

    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
    implementation("net.kyori:adventure-text-serializer-ansi:4.17.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.17.0")
    api("net.kyori:adventure-text-minimessage:4.17.0")

    api("com.github.SkinsRestorer:ConfigMe:beefdbdf7e")
    api("ch.jalu:injector:1.0")

    api("org.incendo:cloud-annotations:2.0.0")
    annotationProcessor("org.incendo:cloud-annotations:2.0.0")
    api("org.incendo:cloud-processors-requirements:1.0.0-SNAPSHOT")
    api("org.incendo:cloud-processors-cooldown:1.0.0-SNAPSHOT")
    api("org.incendo:cloud-brigadier:2.0.0-SNAPSHOT")
    api("org.incendo:cloud-minecraft-extras:2.0.0-SNAPSHOT")
    api("org.incendo:cloud-translations-core:1.0.0-SNAPSHOT")
    api("org.incendo:cloud-translations-minecraft-extras:1.0.0-SNAPSHOT")

    implementation("org.bstats:bstats-base:3.1.0") {
        isTransitive = false
    }

    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")

    testImplementation("org.bstats:bstats-base:3.1.0")

    testImplementation("org.testcontainers:testcontainers:1.20.3")
    testImplementation("org.testcontainers:mariadb:1.20.3")
    testImplementation("org.testcontainers:junit-jupiter:1.20.3")

    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")
}

tasks {
    shadowJar {
        configureKyoriRelocations()
    }
}
