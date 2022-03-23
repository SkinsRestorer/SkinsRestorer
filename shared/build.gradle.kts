dependencies {
    implementation(projects.skinsrestorerApi)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.5")
    implementation("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }

    implementation("net.skinsrestorer:axiom:1.1.1-SNAPSHOT")

    compileOnly("co.aikar:acf-core:0.5.0-SNAPSHOT")
    compileOnly("org.slf4j:slf4j-api:1.7.36")

    compileOnly("com.mojang:authlib:1.11")
    compileOnly("net.md-5:bungeecord-proxy:1.18-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.0.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
