plugins {
    java
    `java-test-fixtures`
    id("io.freefair.lombok")
}

lombok {
    version.set("1.18.22")
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.1")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.7.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")

    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testFixturesImplementation("org.mockito:mockito-core:5.2.0")
    testFixturesImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
