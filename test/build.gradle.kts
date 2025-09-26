plugins {
    id("sr.base-logic")
}

dependencies {
    testFixturesApi(project(":skinsrestorer-shared", "shadow"))

    testImplementation(libs.bstats.base)

    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)

    testRuntimeOnly(libs.postgresql)

    testRuntimeOnly(libs.slf4j.simple)
}
