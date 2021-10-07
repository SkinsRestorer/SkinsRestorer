import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import net.kyori.indra.repository.sonatypeSnapshots

plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("org.cadixdev.licenser") version "0.6.1"
    id("net.kyori.indra") version "2.0.6"
    id("net.kyori.indra.git") version "2.0.6"
    id("net.kyori.indra.publishing") version "2.0.6"
    id("net.kyori.blossom") version "1.3.0"
}

repositories {
    mavenCentral()
    sonatypeSnapshots()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "SpigotMC Repository"
    }
    maven("https://papermc.io/repo/repository/maven-public/") {
        name = "PaperMC Repository"
    }
    maven("https://repo.spongepowered.org/maven") {
        name = "SpongePowered Repository"
    }
    maven("https://repo.codemc.org/repository/maven-public") {
        name = "CodeMC Repository"
    }
    maven("https://repo.aikar.co/content/groups/aikar/") {
        name = "Aikar Repository"
    }
    maven("https://nexus.velocitypowered.com/repository/maven-public/") {
        name = "VelocityPowered Repository"
    }
    maven("https://repo.viaversion.com") {
        name = "ViaVersion Repository"
    }
    maven("https://jitpack.io") {
        name = "JitPack Repository"
    }
    maven("https://libraries.minecraft.net") {
        name = "Minecraft Repository"
    }
}

dependencies {


    shadow("com.github.InventivetalentDev.Spiget-Update:bukkit:1.4.2-SNAPSHOT") {
        exclude("org.bukkit", "bukkit")
    }

    shadow("com.google.code.gson:gson:2.8.8")
    shadow("org.mariadb.jdbc:mariadb-java-client:2.7.4")

    shadow("co.aikar:minecraft-timings:1.0.4")
    shadow("com.gilecode.yagson:j9-reflection-utils:1.0")
    shadow("org.spongepowered:configurate-yaml:4.1.2")
}

group = "net.skinsrestorer"
version = "14.1.6-SNAPSHOT"
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
    include("**/net/skinsrestorer/**")

    header(file("file_header.txt"))
    newLine(false)
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