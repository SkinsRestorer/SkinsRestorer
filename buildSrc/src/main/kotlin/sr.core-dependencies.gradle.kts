plugins {
    java
    `java-test-fixtures`
    id("io.freefair.lombok")
}

dependencies {
    implementation("org.jetbrains:annotations:24.1.0")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.5")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0-SNAPSHOT")
    testFixturesApi("org.junit.jupiter:junit-jupiter-api:5.11.0-SNAPSHOT")
    testFixturesApi("org.mockito:mockito-core:5.12.0")
    testFixturesApi("org.mockito:mockito-junit-jupiter:5.12.0")
}
