plugins {
    java
    `java-test-fixtures`
    id("io.freefair.lombok")
}

dependencies {
    implementation("org.jetbrains:annotations:24.1.0")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.5")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testFixturesImplementation("org.mockito:mockito-core:5.11.0")
    testFixturesImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
}
