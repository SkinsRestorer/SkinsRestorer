plugins {
    java
    id("com.github.johnrengelman.shadow")
}

group = "net.skinsrestorer"
version = "14.1.6-SNAPSHOT"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-proxy:1.17-R0.1-SNAPSHOT")

    shadow("org.bstats:bstats-bungeecord:2.2.1")
    shadow("co.aikar:acf-bungee:0.5.0-SNAPSHOT")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}