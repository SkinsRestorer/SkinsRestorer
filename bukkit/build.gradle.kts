plugins {
    java
    id("com.github.johnrengelman.shadow")
}

group = "net.skinsrestorer"
version = "14.1.6-SNAPSHOT"

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    shadow("io.papermc:paperlib:1.0.6")
    shadow("org.bstats:bstats-bukkit:2.2.1")

    shadow("co.aikar:acf-paper:0.5.0-SNAPSHOT")
    shadow("com.github.cryptomorin:XSeries:8.4.0")
}