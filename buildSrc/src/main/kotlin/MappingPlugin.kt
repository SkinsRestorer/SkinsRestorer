import io.github.patrick.gradle.remapper.RemapTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.withType

open class MappingExtension {
    var mcVersion: String = ""
}

class MappingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("mapping", MappingExtension::class.java)

        project.afterEvaluate {
            val mcVersion = extension.mcVersion
            project.tasks.withType<RemapTask> {
                version.set(mcVersion)
            }

            project.dependencies.add("compileOnly", "org.spigotmc:spigot:$mcVersion-R0.1-SNAPSHOT:remapped-mojang@jar") {
                isTransitive = false
            }
            project.dependencies.add("compileOnly", "org.spigotmc:spigot-api:$mcVersion-R0.1-SNAPSHOT") {
                isTransitive = false
            }
        }
    }
}
