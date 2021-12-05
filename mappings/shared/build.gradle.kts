plugins {
    java
    id("sr.core-dependencies")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.18-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson")
    }
}

java.javaTarget(8)
