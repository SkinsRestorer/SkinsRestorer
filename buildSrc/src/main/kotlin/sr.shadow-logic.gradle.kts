import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("sr.base-logic")
    id("com.gradleup.shadow")
}

tasks {
    processResources {
        expand(
            mapOf(
                "version" to version,
                "description" to description
            )
        )
    }

    jar {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }

    shadowJar {
        mergeServiceFiles()
        configureRelocations()
        failOnDuplicateEntries = true
    }

    build {
        dependsOn(shadowJar)
    }
}

fun ShadowJar.configureRelocations() {
    // Google inject should NOT be relocated
    relocate("com.google.gson", "net.skinsrestorer.shadow.gson")
    relocate("com.google.errorprone", "net.skinsrestorer.shadow.errorprone")

    relocate("com.cryptomorin.xseries", "net.skinsrestorer.shadow.xseries")
    relocate("org.bstats", "net.skinsrestorer.shadow.bstats")

    relocate("org.mariadb.jdbc", "net.skinsrestorer.shadow.mariadb")
    relocate("org.postgresql", "net.skinsrestorer.shadow.postgresql")
    relocate("com.zaxxer.hikari", "net.skinsrestorer.shadow.hikari")

    relocate("org.intellij.lang.annotations", "net.skinsrestorer.shadow.ijannotations")
    relocate("org.jetbrains.annotations", "net.skinsrestorer.shadow.jbannotations")

    relocate("org.yaml.snakeyaml", "net.skinsrestorer.shadow.snakeyaml")
    relocate("ch.jalu.configme", "net.skinsrestorer.shadow.configme")

    relocate("javax.inject", "net.skinsrestorer.shadow.javax.inject")
    relocate("javax.annotation", "net.skinsrestorer.shadow.javax.annotation")
    relocate("ch.jalu.injector", "net.skinsrestorer.shadow.injector")

    relocate("com.github.puregero.multilib", "net.skinsrestorer.shadow.multilib")

    relocate("org.incendo.cloud", "net.skinsrestorer.shadow.cloud")
    relocate("io.leangen.geantyref", "net.skinsrestorer.shadow.geantyref")

    relocate("net.lenni0451.reflect", "net.skinsrestorer.shadow.reflect")
}
