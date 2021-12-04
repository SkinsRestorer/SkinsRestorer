plugins {
    java
    id("sr.core-dependencies")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson")
    }
}

java.javaTarget(8)
