plugins {
    id("sr.base-logic")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson")
    }
}
