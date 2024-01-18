import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

fun ShadowJar.configureKyoriRelocations() {
    relocate("net.kyori", "net.skinsrestorer.shadow.kyori")
}
