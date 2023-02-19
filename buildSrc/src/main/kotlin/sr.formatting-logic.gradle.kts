plugins {
    id("org.cadixdev.licenser")
    id("com.diffplug.spotless")
}

license.include("**/net/skinsrestorer/**")

license.header(rootProject.file("file_header.txt"))
license.newLine(false)

spotless {
    java {
        googleJavaFormat()
    }
}
