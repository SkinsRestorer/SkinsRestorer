import io.papermc.paperweight.util.constants.*
import io.papermc.paperweight.userdev.PaperweightUserExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named

fun Project.publishShadowJar() {
    configurePublication {
        artifact(tasks["shadowJar"])
        artifact(tasks["sourcesJar"])
    }
}

fun Project.publishJavaComponents() {
    configurePublication {
        from(components["java"])
    }
}

private fun Project.configurePublication(configurer: MavenPublication.() -> Unit) {
    extensions.configure<PublishingExtension> {
        publications.named<MavenPublication>("mavenJava") {
            apply(configurer)
        }
    }
}

fun JavaPluginExtension.javaTarget(version: Int) {
    sourceCompatibility = JavaVersion.toVersion(version)
    targetCompatibility = JavaVersion.toVersion(version)
}

fun DependencyHandlerScope.paperDevBundle(
    version: String? = null,
    group: String = "io.papermc.paper",
    artifactId: String = "dev-bundle",
    configuration: String? = null,
    classifier: String? = null,
    ext: String? = null,
    devBundleConfigurationName: String = DEV_BUNDLE_CONFIG,
    configurationAction: ExternalModuleDependency.() -> Unit = {}
): ExternalModuleDependency = devBundleConfigurationName(group, artifactId, version, configuration, classifier, ext, configurationAction)