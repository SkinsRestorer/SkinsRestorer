plugins {
    base
    id("sr.build-logic")
}

allprojects {
    group = "net.skinsrestorer"
    version = "14.1.6-SNAPSHOT"
    description = "Ability to restore/change skins on servers! (Offline and Online Mode)"
}

val platforms = setOf(
    projects.skinsrestorerBukkit,
    projects.skinsrestorerBungee,
    projects.skinsrestorerSponge,
    projects.skinsrestorerVelocity
).map { it.dependencyProject }

val shadow = setOf(
    projects.skinsrestorerShared
).map { it.dependencyProject }

val special = setOf(
    projects.skinsrestorer,
    projects.skinsrestorerApi
).map { it.dependencyProject }

subprojects {
    when (this) {
        in platforms -> plugins.apply("sr.platform-logic")
        in shadow -> plugins.apply("sr.shadow-logic")
        in special -> plugins.apply("sr.base-logic")
    }
}

