plugins {
    java
    `java-test-fixtures`
    id("io.freefair.lombok")
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.1")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.7.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")

    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testFixturesImplementation("org.mockito:mockito-core:5.3.1")
    testFixturesImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
}
