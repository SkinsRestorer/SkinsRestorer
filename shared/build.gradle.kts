plugins {
    java
    id("com.github.johnrengelman.shadow")
}

group = "net.skinsrestorer"
version = "14.1.6-SNAPSHOT"

dependencies {
    implementation("org.jetbrains:annotations:22.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    shadow("org.spongepowered:configurate-yaml:4.1.2")
    shadow("com.google.code.gson:gson:2.8.8")
    shadow("org.mariadb.jdbc:mariadb-java-client:2.7.4")

    shadow("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }

    compileOnly("co.aikar:acf-core:0.5.0-SNAPSHOT")
    compileOnly("org.slf4j:slf4j-api:1.7.32")

    compileOnly(project(":api"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}