plugins {
    id("sr.base-logic")
}

dependencies {
    implementation("com.github.puregero:multilib:1.1.13")
    compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT") {
        isTransitive = false
    }
}
