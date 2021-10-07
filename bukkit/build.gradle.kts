plugins {
    java
    id("com.github.johnrengelman.shadow")
}

group = "net.skinsrestorer"
version = "14.1.6-SNAPSHOT"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    shadow("io.papermc:paperlib:1.0.6")
    shadow("org.bstats:bstats-bukkit:2.2.1")

    shadow("co.aikar:acf-paper:0.5.0-SNAPSHOT")
    shadow("com.github.cryptomorin:XSeries:8.4.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}