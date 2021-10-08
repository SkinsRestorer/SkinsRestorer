plugins {
    java
    id("com.github.johnrengelman.shadow")
}

group = "net.skinsrestorer"
version = "14.1.6-SNAPSHOT"

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")

    shadow("io.papermc:paperlib:1.0.6")
    shadow("org.bstats:bstats-bukkit:2.2.1")

    shadow("co.aikar:acf-paper:0.5.0-SNAPSHOT")
    shadow("com.github.cryptomorin:XSeries:8.4.0")

    shadow("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }

    compileOnly("com.viaversion:viabackwards-common:4.0.1")
    compileOnly("com.viaversion:viaversion:4.0.0")

    compileOnly("com.mojang:authlib:1.11")

    implementation(project(":api"))
    implementation(project(":shared"))
}