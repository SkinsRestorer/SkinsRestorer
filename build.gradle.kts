import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import net.kyori.indra.repository.sonatypeSnapshots

plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.cadixdev.licenser") version "0.6.1"
    id("net.kyori.indra") version "2.0.6"
    id("net.kyori.indra.git") version "2.0.6"
    id("net.kyori.indra.publishing") version "2.0.6"
    id("net.kyori.indra.license-header") version "2.0.6"
    id("net.kyori.blossom") version "1.3.0"
}

repositories {
    mavenCentral()
    sonatypeSnapshots()
    maven {
        name = "SpigotMC Repository"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "PaperMC Repository"
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
    maven {
        name = "SpongePowered Repository"
        url = uri("https://repo.spongepowered.org/maven")
    }
    maven {
        name = "CodeMC Repository"
        url = uri("https://repo.codemc.org/repository/maven-public")
    }
    maven {
        name = "Aikar Repository"
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }
    maven {
        name = "VelocityPowered Repository"
        url = uri("https://nexus.velocitypowered.com/repository/maven-public/")
    }
    maven {
        name = "ViaVersion Repository"
        url = uri("https://repo.viaversion.com")
    }
    maven {
        name = "JitPack Repository"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "Minecraft Repository"
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    shadow("org.bstats:bstats-bukkit:2.2.1")
    shadow("org.bstats:bstats-bungeecord:2.2.1")
    shadow("org.bstats:bstats-sponge:2.2.1")
    shadow("org.bstats:bstats-velocity:2.2.1")
    shadow("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }
    shadow("co.aikar:acf-paper:0.5.0-SNAPSHOT")
    shadow("co.aikar:acf-bungee:0.5.0-SNAPSHOT")
    shadow("co.aikar:acf-sponge:0.5.0-SNAPSHOT")
    shadow("com.github.AlexProgrammerDE.commands:acf-velocity:4da0ffec3c")
    shadow("com.google.code.gson:gson:2.8.8")
    shadow("mysql:mysql-connector-java:8.0.26")
    shadow("com.github.cryptomorin:XSeries:8.4.0")
    shadow("io.papermc:paperlib:1.0.6")
    shadow("co.aikar:minecraft-timings:1.0.4")
    shadow("com.gilecode.yagson:j9-reflection-utils:1.0")
    shadow("org.spongepowered:configurate-yaml:4.1.2")

    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-proxy:1.17-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:1.11")
    compileOnly("com.viaversion:viabackwards-common:4.0.1")
    compileOnly("com.viaversion:viaversion:4.0.0")

    compileOnly("com.velocitypowered:velocity-api:3.0.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.0.1")

    compileOnly("org.spongepowered:spongeapi:7.3.0")
    annotationProcessor("org.spongepowered:spongeapi:7.3.0")

    compileOnly("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.20")
}

group = "net.skinsrestorer"
version = "14.1.5"
description = "Ability to restore/change skins on servers! (Offline and Online Mode)"
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

indra {
    includeJavaSoftwareComponentInPublications()
    github("SkinsRestorer", "SkinsRestorerX") {
        ci(true)
    }
    gpl3OnlyLicense()
    publishReleasesTo("codemc-releases", "https://repo.codemc.org/repository/maven-releases/")
    publishSnapshotsTo("codemc-snapshots", "https://repo.codemc.org/repository/maven-snapshots/")

    configurePublications {
        artifact("build/libs/SkinsRestorer.jar") {
            extension = "jar"
        }
        pom {
            name.set("SkinsRestorer")
            url.set("https://skinsrestorer.net/")
            organization {
                name.set("SkinsRestorer")
                url.set("https://skinsrestorer.net")
            }
            developers {
                developer {
                    id.set("xknat")
                    timezone.set("Europe/Amsterdam")
                }
                developer {
                    id.set("AlexProgrammerDE")
                    timezone.set("Europe/Berlin")
                    url.set("https://pistonmaster.net")
                }
            }
        }

        versionMapping {
            usage(Usage.JAVA_API) { fromResolutionOf(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) }
            usage(Usage.JAVA_RUNTIME) { fromResolutionResult() }
        }
    }
}

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("CODEMC_USERNAME")
                password = System.getenv("CODEMC_PASSWORD")
            }
            name = "codemc"
            val repoName = if (version.toString().endsWith("SNAPSHOT")) "maven-snapshots" else "maven-releases"
            url = uri("https://repo.codemc.org/repository/${repoName}/")
        }
    }
}

license {
    includes.remove("**/*.java")
    includes.add("src/main/java/net/skinsrestorer/**/*.java")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

blossom {
    replaceTokenIn("src/main/java/net/skinsrestorer/velocity/SkinsRestorer.java")
    replaceTokenIn("src/main/java/net/skinsrestorer/sponge/SkinsRestorer.java")

    replaceToken("{version}", version)
    replaceToken("{description}", rootProject.description)
    replaceToken("{url}", "https://skinsrestorer.net/")
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

    compileJava {
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("-Xlint:-processing")
        options.isFork = true
    }

    create<ConfigureShadowRelocation>("relocateShadowJar") {
        target = shadowJar.get()
        prefix = "net.skinsrestorer.shadow"
    }

    javadoc {
        exclude("li/cock/ie/**")
        title = "SkinsRestorer Javadocs"
    }
}
