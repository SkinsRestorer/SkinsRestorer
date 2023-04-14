dependencies {
    api(projects.skinsrestorerBuildData)
    implementation(projects.skinsrestorerApi)

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.1.3") {
        exclude("com.github.waffle", "waffle-jna")
    }

    implementation("org.fusesource.jansi:jansi:2.4.0")

    api("com.github.SkinsRestorer:ConfigMe:beefdbdf7e")
    api("ch.jalu:injector:1.0")

    compileOnly("org.bstats:bstats-base:3.0.2")

    implementation("com.mojang:brigadier:1.1.8")

    testImplementation("org.bstats:bstats-base:3.0.2")
}
