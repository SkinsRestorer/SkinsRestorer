dependencies {
    api(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.1.3") {
        exclude("com.github.waffle", "waffle-jna")
    }

    implementation("org.fusesource.jansi:jansi:2.4.0")

    api("com.github.SkinsRestorer:ConfigMe:beefdbdf7e")
    api("net.skinsrestorer:axiom:1.1.2-SNAPSHOT")
    api("ch.jalu:injector:1.0")

    implementation("com.github.aikar:locales:5f204c3afb")
    implementation("org.bstats:bstats-base:3.0.2")

    compileOnly("com.github.SkinsRestorer.commands:acf-core:ebc273d2f3")

    testRuntimeOnly("com.github.SkinsRestorer.commands:acf-core:ebc273d2f3")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
