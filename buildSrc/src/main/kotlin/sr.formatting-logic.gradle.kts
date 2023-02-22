import com.github.spotbugs.snom.Confidence

plugins {
    id("org.cadixdev.licenser")
    id("com.diffplug.spotless")
    id("com.github.spotbugs")
}

license.include("**/net/skinsrestorer/**")

license.header(rootProject.file("file_header.txt"))
license.newLine(false)

spotbugs {
    toolVersion.set("4.7.3")
    // ignoreFailures.set(false)    // bug free or it doesn't ship!
    reportLevel.set(Confidence.MEDIUM)    // low|medium|high (low = sensitive to even minor mistakes)
    omitVisitors.set(listOf("FindReturnRef")) // https://spotbugs.readthedocs.io/en/latest/detectors.html#findreturnref
}

spotless {
    java {
        target("**/net/skinsrestorer/**")

        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()
    }
}
