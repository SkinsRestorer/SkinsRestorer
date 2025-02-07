plugins {
    id("sr.shadow-logic")
}

dependencies {
    implementation(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)
    api(project(":multiver:kyori", "shadow")) {
        exclude("org.google.code.gson", "gson")
    }

    api("com.google.code.gson:gson:2.12.1")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.1") {
        exclude("com.github.waffle", "waffle-jna")
    }

    api("com.github.SkinsRestorer:ConfigMe:beefdbdf7e")
    api("ch.jalu:injector:1.0")

    api("org.incendo:cloud-annotations:2.0.0")
    annotationProcessor("org.incendo:cloud-annotations:2.0.0")
    api("org.incendo:cloud-processors-requirements:1.0.0-SNAPSHOT")
    api("org.incendo:cloud-processors-cooldown:1.0.0-SNAPSHOT")
    api("org.incendo:cloud-brigadier:2.0.0-SNAPSHOT")
    api("org.incendo:cloud-translations-core:1.0.0-SNAPSHOT")

    implementation("org.bstats:bstats-base:3.1.0") {
        isTransitive = false
    }

    compileOnly("net.kyori:adventure-text-minimessage:4.18.0")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.2.3")

    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")

    testImplementation("org.bstats:bstats-base:3.1.0")

    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:mariadb:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")

    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")
}
