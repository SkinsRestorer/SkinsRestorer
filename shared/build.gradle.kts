plugins {
    id("java-library")
    id("com.github.johnrengelman.shadow")
}

group = "net.skinsrestorer"
version = "14.1.6-SNAPSHOT"

dependencies {
    api(project(":api"))

    api("org.jetbrains:annotations:22.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    api("org.spongepowered:configurate-yaml:4.1.2")
    api("com.google.code.gson:gson:2.8.8")
    api("com.google.guava:guava:31.0.1-jre")
    api("org.mariadb.jdbc:mariadb-java-client:2.7.4")

    shadow("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }

    compileOnly("co.aikar:acf-core:0.5.0-SNAPSHOT")
    compileOnly("org.slf4j:slf4j-api:1.7.32")

    compileOnly("com.mojang:authlib:1.11")
    compileOnly("net.md-5:bungeecord-proxy:1.17-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.0.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
