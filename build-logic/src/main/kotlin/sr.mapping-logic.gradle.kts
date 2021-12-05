plugins {
    java
    id("sr.license-logic")
    id("sr.core-dependencies")
    id("io.papermc.paperweight.userdev")
}

dependencies.implementation(project(":mappings:shared"))

tasks.named("build").get().dependsOn("reobfJar")