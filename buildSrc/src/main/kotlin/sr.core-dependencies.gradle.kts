plugins {
    java
    `java-test-fixtures`
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    implementation("org.jetbrains:annotations:24.0.1")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.7.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.8.0")

    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testFixturesImplementation("org.mockito:mockito-core:4.8.0")
    testFixturesImplementation("org.mockito:mockito-junit-jupiter:4.8.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
