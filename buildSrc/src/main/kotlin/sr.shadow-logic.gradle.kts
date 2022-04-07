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
    relocate("co.aikar.timings.lib", "net.skinsrestorer.shadow.aikar.timings")
    relocate("com.google.gson", "net.skinsrestorer.shadow.google.gson")
    relocate("com.google.common", "net.skinsrestorer.shadow.google.common")
    relocate("com.google.j2objc.annotations", "net.skinsrestorer.shadow.google.j2objc.annotations")
    relocate("com.google.errorprone.annotation", "net.skinsrestorer.shadow.google.errorprone.annotation")
    relocate("com.cryptomorin.xseries", "net.skinsrestorer.shadow.xseries")
    relocate("org.bstats", "net.skinsrestorer.shadow.bstats")
    relocate("io.papermc.lib", "net.skinsrestorer.shadow.paperlib")
    relocate("io.leangen.geantyref", "net.skinsrestorer.shadow.geantyref")
    relocate("org.intellij.lang.annotations", "net.skinsrestorer.shadow.ijannotations")
    relocate("org.inventivetalent.update.spiget", "net.skinsrestorer.shadow.spiget")
    relocate("org.jetbrains.annotations", "net.skinsrestorer.shadow.jbannotations")
    relocate("org.mariadb.jdbc", "net.skinsrestorer.shadow.mariadb")
    relocate("org.spongepowered.configurate", "net.skinsrestorer.shadow.configurate")
    relocate("org.yaml.snakeyaml", "net.skinsrestorer.shadow.snakeyaml")
    relocate("org.checkerframework", "net.skinsrestorer.shadow.checkerframework")
    relocate("javax.annotation", "net.skinsrestorer.shadow.javaxannotation")
    relocate("org.fusesource.jansi", "net.skinsrestorer.shadow.jansi")
}
