dependencies {
    implementation(projects.skinsrestorerApi)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    implementation("com.google.code.gson:gson:2.9.1")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.0.7")
    api("com.github.InventivetalentDev.Spiget-Update:core:1.4.6-SNAPSHOT")

    implementation("org.fusesource.jansi:jansi:2.4.0")

    implementation("net.skinsrestorer:axiom:1.1.2-SNAPSHOT")

    compileOnly("co.aikar:acf-core:0.5.1-SNAPSHOT")
    compileOnly("org.slf4j:slf4j-api:2.0.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
