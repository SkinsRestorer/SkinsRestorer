plugins {
    java
    id("io.papermc.paperweight.userdev")
}

tasks {
    build {
        dependsOn(reobfJar)
    }
}