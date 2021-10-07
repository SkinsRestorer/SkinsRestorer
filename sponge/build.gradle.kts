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

    compileOnly("org.spongepowered:spongeapi:7.3.0")
    annotationProcessor("org.spongepowered:spongeapi:7.3.0")

    shadow("org.bstats:bstats-sponge:2.2.1")
    shadow("co.aikar:acf-sponge:0.5.0-SNAPSHOT")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}