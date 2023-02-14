plugins {
    java
}

dependencies.compileOnly("org.projectlombok:lombok:1.18.26")
dependencies.annotationProcessor("org.projectlombok:lombok:1.18.26")
dependencies.implementation("org.jetbrains:annotations:24.0.0")

dependencies.testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
dependencies.testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
dependencies.testImplementation("org.mockito:mockito-core:4.8.0")
dependencies.testImplementation("org.mockito:mockito-inline:4.8.0")

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
