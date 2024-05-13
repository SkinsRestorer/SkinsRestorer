plugins {
    id("sr.shadow-logic")
}

dependencies {
    implementation(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)

    api("com.google.code.gson:gson:2.10.1")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3") {
        exclude("com.github.waffle", "waffle-jna")
    }

    implementation("net.kyori:adventure-api:4.16.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.16.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.16.0")
    implementation("net.kyori:adventure-text-serializer-ansi:4.16.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.17.0")
    api("net.kyori:adventure-text-minimessage:4.16.0")

    api("com.github.SkinsRestorer:ConfigMe:beefdbdf7e")
    api("ch.jalu:injector:1.0")

    compileOnly("org.bstats:bstats-base:3.0.2") {
        isTransitive = false
    }

    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")

    implementation(libs.brigadier)

    testImplementation("org.bstats:bstats-base:3.0.2")

    testImplementation("org.testcontainers:testcontainers:1.19.7")
    testImplementation("org.testcontainers:mariadb:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")

    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.13")
}

tasks {
    shadowJar {
        configureKyoriRelocations()
    }
}
