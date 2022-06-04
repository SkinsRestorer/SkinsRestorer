plugins {
    java
    id("sr.license-logic")
    id("sr.core-dependencies")
    id("io.github.patrick.remapper")
}

dependencies.implementation(project(":mappings:shared"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named("remap").get().dependsOn("jar")
tasks.named("build").get().dependsOn("remap")

configurations {
    create("reobfuscated") {
        isCanBeResolved = false
        isCanBeConsumed = true
        outgoing.artifact(tasks.jar.get().archiveFile)
    }
}
