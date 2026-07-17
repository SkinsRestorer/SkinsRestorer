plugins {
    base
    idea
    eclipse
}

tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion = JavaLanguageVersion.of(25)
}

allprojects {
    group = "net.skinsrestorer"
    version = property("maven_version")!!
    description = "Ability to restore/change skins on servers!"

    repositories {
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            name = "SpigotMC Repository"
            content {
                includeGroup("org.spigotmc")
                includeGroup("net.md-5")
            }
            mavenContent { snapshotsOnly() }
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "PaperMC Repository"
            content {
                includeGroup("io.papermc.paper")
                includeGroup("com.velocitypowered")
                includeModule("net.md-5", "bungeecord-chat")
                // TODO: Remove, for some reason not in sonatype
                includeGroup("org.incendo")
            }
        }
        maven("https://repo.codemc.org/repository/nms/") {
            name = "CodeMC NMS Repository"
            content {
                includeGroup("org.spigotmc")
                includeGroup("org.bukkit")
            }
        }
        maven("https://repo.viaversion.com/") {
            name = "ViaVersion Repository"
            content {
                includeGroup("com.viaversion")
            }
        }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
            name = "PlaceholderAPI Repository"
            content {
                includeGroup("me.clip")
            }
        }
        maven("https://repo.clojars.org/") {
            name = "Clojars Repository"
            content {
                includeGroup("com.github.puregero")
            }
        }
        maven("https://jitpack.io/") {
            name = "JitPack Repository"
            content {
                includeGroupByRegex("com\\.github\\..*")
                excludeGroup("com.github.cryptomorin")
                excludeGroup("com.github.puregero")
            }
        }
        maven("https://libraries.minecraft.net/") {
            name = "Minecraft Repository"
            content {
                includeGroup("net.minecraft")
                includeGroup("com.mojang")
            }
        }
        maven("https://repo.opencollab.dev/maven-snapshots/") {
            name = "OpenCollab Snapshot Repository"
            content {
                includeGroupByRegex("org\\.geysermc\\..*")
            }
            mavenContent { snapshotsOnly() }
        }
        maven("https://repo.opencollab.dev/maven-releases/") {
            name = "OpenCollab Release Repository"
            content {
                includeGroupByRegex("org\\.geysermc\\..*")
            }
            mavenContent { releasesOnly() }
        }
        maven("https://maven.wagyourtail.xyz/releases") {
            name = "WagYourTail Release Repository"
            content {
                includeGroup("xyz.wagyourtail")
            }
            mavenContent { releasesOnly() }
        }
        maven("https://maven.wagyourtail.xyz/snapshots") {
            name = "WagYourTail Snapshot Repository"
            content {
                includeGroup("xyz.wagyourtail")
            }
            mavenContent { snapshotsOnly() }
        }
        maven("https://repo.fandmc.cn/repository/maven-public/") {
            name = "FandMC Repository"
            content {
                includeGroup("io.fand")
            }
        }
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            name = "Sonatype Snapshot Repository"
            mavenContent { snapshotsOnly() }
        }
        mavenCentral()
    }
}
