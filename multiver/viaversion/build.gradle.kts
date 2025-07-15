plugins {
    id("sr.base-logic")
}

dependencies {
    compileOnly(libs.viabackwards) {
        isTransitive = false
    }
    compileOnly(libs.viaversion) {
        isTransitive = false
    }
}
