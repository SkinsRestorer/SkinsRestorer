plugins {
    id("org.cadixdev.licenser")
}

license.include("**/net/skinsrestorer/**")
license.exclude("**/net/skinsrestorer/shared/reflection/access/**")
license.exclude("**/net/skinsrestorer/shared/reflection/reflect/**")

license.header(rootProject.file("file_header.txt"))
license.newLine(false)
