plugins {
    base
    idea
    eclipse
}

allprojects {
    group = "net.skinsrestorer"
    version = "15.5.2-SNAPSHOT"
    description = "Ability to restore/change skins on servers!"

    repositories {
        maven("https://maven.architectury.dev/") {
            name = "Architectury Repository"
        }
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            name = "SpigotMC Repository"
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "PaperMC Repository"
        }
        maven("https://repo.codemc.org/repository/maven-public/") {
            name = "CodeMC Repository"
        }
        maven("https://repo.codemc.org/repository/nms/") {
            name = "CodeMC NMS Repository"
        }
        maven("https://repo.viaversion.com/") {
            name = "ViaVersion Repository"
        }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
            name = "PlaceholderAPI Repository"
        }
        maven("https://jitpack.io/") {
            name = "JitPack Repository"
        }
        maven("https://libraries.minecraft.net/") {
            name = "Minecraft Repository"
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            name = "Sonatype Repository"
        }
        maven("https://repo.clojars.org/") {
            name = "Clojars Repository"
        }
        maven("https://repo.opencollab.dev/maven-snapshots/") {
            name = "OpenCollab Snapshot Repository"
        }
        maven("https://repo.opencollab.dev/maven-releases/") {
            name = "OpenCollab Release Repository"
        }
        maven("https://maven.wagyourtail.xyz/releases") {
            name = "WagYourTail Release Repository"
        }
        maven("https://maven.wagyourtail.xyz/snapshots") {
            name = "WagYourTail Snapshot Repository"
        }
        mavenCentral()
    }
}
