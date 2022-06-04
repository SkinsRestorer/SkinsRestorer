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

configurations {
    create("reobfuscated") {
        isCanBeResolved = false
        isCanBeConsumed = true
        outgoing.artifact(tasks.jar.get().archiveFile)
    }
}
