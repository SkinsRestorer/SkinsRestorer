plugins {
    java
    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    compileOnly("com.velocitypowered:velocity-api:3.0.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.0.1")

    shadow("org.bstats:bstats-velocity:2.2.1")
    shadow("com.github.AlexProgrammerDE.commands:acf-velocity:4da0ffec3c")

    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    shadow("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }
}
