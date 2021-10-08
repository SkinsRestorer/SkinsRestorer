import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

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
        dependsOn(getByName("relocateShadowJar") as ConfigureShadowRelocation)
        exclude("META-INF/SPONGEPO.SF", "META-INF/SPONGEPO.DSA", "META-INF/SPONGEPO.RSA")
        minimize()
        archiveFileName.set("SkinsRestorer.jar")
    }

    build {
        dependsOn(shadowJar)
    }

    create<ConfigureShadowRelocation>("relocateShadowJar") {
        target = shadowJar.get()
        prefix = "net.skinsrestorer.shadow"
    }
}

publishShadowJar()