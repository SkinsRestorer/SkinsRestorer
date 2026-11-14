plugins {
    id("sr.base-logic")
}

dependencies {
    testFixturesApi(project(":skinsrestorer-shared", "shadow"))

    testImplementation(libs.bstats.base)

    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)

    testRuntimeOnly(libs.postgresql)

    testRuntimeOnly(libs.slf4j.simple)
}

val java8Probe = sourceSets.create("java8Probe")
tasks.named<JavaCompile>(java8Probe.compileJavaTaskName) {
    options.release.set(8)
}

val packagedPlugin = rootProject.layout.buildDirectory.file("libs/SkinsRestorer.jar")
tasks.test {
    dependsOn(java8Probe.classesTaskName, ":skinsrestorer:shadeDowngradedApi")
    inputs.file(packagedPlugin)
    inputs.files(java8Probe.output)
    systemProperty("skinsrestorer.packagedPlugin", packagedPlugin.get().asFile.absolutePath)
    systemProperty("skinsrestorer.java8ProbeClasspath", java8Probe.output.classesDirs.asPath)
    systemProperty("skinsrestorer.java8Executable", javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    }.get().executablePath.asFile.absolutePath)
}
