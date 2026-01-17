import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType

interface MappingExtension {
    val mcVersion: Property<String>
}

class MappingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Get existing extension or create new one
        val extension = project.extensions.findByType<MappingExtension>()
            ?: project.extensions.create("mapping", MappingExtension::class.java)

        project.tasks.withType<SpigotRemapTask>().configureEach {
            version.set(extension.mcVersion)
        }

        // Use dependency constraints with provider to avoid afterEvaluate
        project.dependencies {
            addProvider("compileOnly", extension.mcVersion.map { mcVersion ->
                (project.dependencies.create("org.spigotmc:spigot:$mcVersion-R0.1-SNAPSHOT:remapped-mojang@jar") as ExternalModuleDependency).apply {
                    isTransitive = false
                }
            })
            addProvider("compileOnly", extension.mcVersion.map { mcVersion ->
                (project.dependencies.create("org.spigotmc:spigot-api:$mcVersion-R0.1-SNAPSHOT") as ExternalModuleDependency).apply {
                    isTransitive = false
                }
            })
        }
    }
}
