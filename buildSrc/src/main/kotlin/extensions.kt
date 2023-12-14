import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension

fun JavaPluginExtension.javaTarget(version: Int) {
    sourceCompatibility = JavaVersion.toVersion(version)
    targetCompatibility = JavaVersion.toVersion(version)
}

fun ShadowJar.configureKyoriRelocations() {
    relocate("net.kyori", "net.skinsrestorer.shadow.kyori")
}
