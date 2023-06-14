dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    implementation(projects.mappings.mcShared)
    implementation(projects.multiver.bukkit.shared)
    implementation(projects.multiver.bukkit.spigot)
    implementation(projects.multiver.bukkit.paper)
    implementation(projects.multiver.bukkit.multipaper)
    implementation(projects.multiver.bukkit.v17)

    setOf("1-18", "1-18-2", "1-19", "1-19-3", "1-19-4", "1-20").forEach {
        implementation(project(":mappings:mc-$it", "remapped"))
    }
    testImplementation(testFixtures(projects.skinsrestorerShared))

    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson")
    }

    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("com.github.cryptomorin:XSeries:9.4.0")

    compileOnly("com.viaversion:viabackwards-common:4.7.0")
    compileOnly("com.viaversion:viaversion:4.4.1")
    compileOnly("com.mojang:authlib:1.11")

    testImplementation("org.spigotmc:spigot-api:1.19-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson")
    }
    testRuntimeOnly("com.mojang:authlib:1.11")
}

val projectIncludes = setOf(
    rootProject.projects.multiver.bukkit.folia
).map { it.dependencyProject }

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    projectIncludes.forEach { include ->
        val jarTask = include.tasks.named<Jar>("jar").get()
        dependsOn(jarTask)
        from(zipTree(jarTask.archiveFile))
    }
}

tasks.shadowJar {
    projectIncludes.forEach { include ->
        val jarTask = include.tasks.named<Jar>("jar").get()
        dependsOn(jarTask)
        from(zipTree(jarTask.archiveFile))
    }
}
