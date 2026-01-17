plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        target("**/net/skinsrestorer/**")

        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()

        licenseHeaderFile(rootProject.layout.projectDirectory.file("file_header.txt"))
    }
}
