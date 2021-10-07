plugins {
    id("java")
}

group = "net.skinsrestorer"
version = "14.1.6-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))
}