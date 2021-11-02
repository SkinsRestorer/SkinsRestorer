plugins {
    id("org.cadixdev.licenser")
}

license.include("**/net/skinsrestorer/**")
license.exclude("**/net/skinsrestorer/api/reflection/access/**")
license.exclude("**/net/skinsrestorer/api/reflection/reflect/**")

license.header(rootProject.file("file_header.txt"))
license.newLine(false)
