plugins {
    base
    idea
}

allprojects {
    group = "net.skinsrestorer"
    version = "15.0.0"
    description = "Ability to restore/change skins on servers!"
}

val platforms = setOf(
    projects.skinsrestorerBukkit,
    projects.skinsrestorerBungee,
    projects.skinsrestorerSponge,
    projects.skinsrestorerVelocity
).map { it.dependencyProject }

val shadow = setOf(
    projects.skinsrestorerShared,
).map { it.dependencyProject }

val special = setOf(
    projects.skinsrestorer,
    projects.skinsrestorerApi,
    projects.multiver.bukkit.folia
).map { it.dependencyProject }

val mappings = setOf(
    projects.mappings
).map { it.dependencyProject }

subprojects {
    when (this) {
        in platforms -> plugins.apply("sr.platform-logic")
        in shadow -> plugins.apply("sr.shadow-logic")
        in special -> plugins.apply("sr.base-logic")
        in mappings -> subprojects.onEach {
            if (!it.name.contains("shared")) it.plugins.apply("sr.mapping-logic")
        }
    }
}
