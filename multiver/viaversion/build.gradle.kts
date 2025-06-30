plugins {
    id("sr.base-logic")
}

dependencies {
    compileOnly("com.viaversion:viabackwards-common:5.4.1") {
        isTransitive = false
    }
    compileOnly("com.viaversion:viaversion:5.0.0") {
        isTransitive = false
    }
}
