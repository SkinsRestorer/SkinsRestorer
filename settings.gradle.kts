rootProject.name = "skinsrestorer"

plugins {
    id("com.gradle.enterprise") version("3.7")
}

gradleEnterprise {
    buildScan {
        publishAlwaysIf(!System.getenv("CI").isNullOrEmpty())
    }
}