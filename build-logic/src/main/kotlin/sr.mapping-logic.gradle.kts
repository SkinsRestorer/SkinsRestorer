plugins {
    java
    id("io.papermc.paperweight.userdev")
}

dependencies {
    paperDevBundle("1.18-R0.1-SNAPSHOT")
}

tasks {
    build {
        dependsOn(reobfJar)
    }
}