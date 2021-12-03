plugins {
    java
    id("sr.license-logic")
    id("sr.core-dependencies")
    id("io.papermc.paperweight.userdev")
}

tasks.named("build").get().dependsOn("reobfJar")