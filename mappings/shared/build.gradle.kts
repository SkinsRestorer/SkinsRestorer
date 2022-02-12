plugins {
    id("sr.base-logic")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.18-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson")
    }
}
