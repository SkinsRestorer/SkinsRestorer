plugins {
    java
    id("com.github.johnrengelman.shadow")
}

group = "net.skinsrestorer"
version = "14.1.6-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    implementation(project(":api"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}