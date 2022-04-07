import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("sr.base-logic")
    id("com.github.johnrengelman.shadow")
}

tasks {
    processResources {
        expand("version" to version, "description" to description)
    }

    jar {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }

    shadowJar {
        exclude("META-INF/SPONGEPO.SF", "META-INF/SPONGEPO.DSA", "META-INF/SPONGEPO.RSA")
        minimize()
        configureRelocations()
    }

    build {
        dependsOn(shadowJar)
    }
}

fun ShadowJar.configureRelocations() {
    relocate("co.aikar.commands", "net.skinsrestorer.shadow.aikar.commands")
    relocate("co.aikar.locales", "net.skinsrestorer.shadow.aikar.locales")

    relocate("com.google.gson", "net.skinsrestorer.shadow.google.gson")
    relocate("com.cryptomorin.xseries", "net.skinsrestorer.shadow.xseries")
    relocate("org.bstats", "net.skinsrestorer.shadow.bstats")
    relocate("io.papermc.lib", "net.skinsrestorer.shadow.paperlib")
    relocate("org.inventivetalent.update.spiget", "net.skinsrestorer.shadow.spiget")
    relocate("org.mariadb.jdbc", "net.skinsrestorer.shadow.mariadb")
    relocate("org.yaml.snakeyaml", "net.skinsrestorer.shadow.snakeyaml")

    relocate("org.intellij.lang.annotations", "net.skinsrestorer.shadow.ijannotations")
    relocate("org.jetbrains.annotations", "net.skinsrestorer.shadow.jbannotations")
    relocate("org.fusesource.jansi", "net.skinsrestorer.shadow.jansi")
    relocate("org.apache.commons.lang3", "net.skinsrestorer.shadow.commons.lang3")
}
