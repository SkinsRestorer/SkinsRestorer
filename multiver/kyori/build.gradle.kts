plugins {
    id("sr.base-logic")
    id("com.gradleup.shadow")
}

dependencies {
    api("net.kyori:adventure-api:4.18.0")
    api("net.kyori:adventure-text-serializer-gson:4.18.0")
    api("net.kyori:adventure-text-serializer-legacy:4.18.0")
    api("net.kyori:adventure-text-serializer-ansi:4.18.0")
    api("net.kyori:adventure-text-serializer-plain:4.18.0")
    api("net.kyori:adventure-text-minimessage:4.18.0")

    api(libs.adventure.bukkit)
    api(libs.adventure.bungeecord)

    // As it uses adventure, we need to relocate it
    api("org.incendo:cloud-minecraft-extras:2.0.0-SNAPSHOT")
    api("org.incendo:cloud-translations-minecraft-extras:1.0.0-SNAPSHOT")
}

tasks {
    shadowJar {
        relocate("net.kyori", "net.skinsrestorer.shadow.kyori")
        dependencies {
            exclude {
                it.moduleGroup != "net.kyori"
                        && it.moduleGroup != "com.github.KyoriPowered.adventure-platform"
                        && it.moduleGroup != "org.incendo"
            }
        }
    }
}
