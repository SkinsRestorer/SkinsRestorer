import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

plugins {
    id("sr.base-logic")
    id("com.github.johnrengelman.shadow")
}

tasks {
    processResources {
        expand("version" to version, "description" to description)
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        dependsOn(getByName("relocateShadowJar") as ConfigureShadowRelocation)
        exclude("META-INF/SPONGEPO.SF", "META-INF/SPONGEPO.DSA", "META-INF/SPONGEPO.RSA")
        minimize()
        configurations = listOf(project.configurations.shadow.get())
        archiveFileName.set("SkinsRestorer.jar")
    }

    jar {
        enabled = false
    }

    create<ConfigureShadowRelocation>("relocateShadowJar") {
        target = shadowJar.get()
        prefix = "net.skinsrestorer.shadow"
    }
}
