plugins {
    java
    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    compileOnly("org.spongepowered:spongeapi:7.3.0")
    annotationProcessor("org.spongepowered:spongeapi:7.3.0")

    shadow("org.bstats:bstats-sponge:2.2.1")
    shadow("co.aikar:acf-sponge:0.5.0-SNAPSHOT")

    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)

    shadow("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }

}
