/*
 * Configuration cache compatible Spigot remapping task.
 * Inspired by io.github.patrick.remapper but without storing task references.
 */

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

abstract class SpigotRemapTask @Inject constructor(
    objects: ObjectFactory,
    private val layout: ProjectLayout
) : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:Input
    @get:Optional
    abstract val action: Property<RemapAction>

    @get:Input
    @get:Optional
    abstract val skip: Property<Boolean>

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val archiveClassifier: Property<String>

    @get:Input
    @get:Optional
    abstract val archiveName: Property<String>

    @get:Optional
    @get:OutputDirectory
    abstract val archiveDirectory: DirectoryProperty

    @get:InputFiles
    abstract val mappingFiles: ConfigurableFileCollection

    @get:InputFiles
    abstract val inheritanceFiles: ConfigurableFileCollection

    @get:OutputFile
    val outputFile: RegularFileProperty = objects.fileProperty()

    @get:Internal
    abstract val projectName: Property<String>

    init {
        group = "build"
        description = "Remaps jar from Mojang mappings to Spigot mappings"

        // Set default output file based on input
        outputFile.convention(inputFile.zip(archiveClassifier) { input, classifier ->
            val baseName = input.asFile.nameWithoutExtension
            layout.buildDirectory.file("libs/$baseName-$classifier.jar").get()
        })
    }

    @TaskAction
    fun execute() {
        if (skip.orNull == true) {
            logger.lifecycle("Skipping remap task")
            return
        }

        val inputJarFile = inputFile.get().asFile
        val targetFile = outputFile.get().asFile

        var fromFile = inputJarFile
        var toFile = Files.createTempFile(null, ".jar").toFile()

        val remapAction = action.getOrElse(RemapAction.MOJANG_TO_SPIGOT)
        val procedures = remapAction.procedures
        val mappingFilesList = mappingFiles.files.toList()
        val inheritanceFilesList = inheritanceFiles.files.toList()

        if (mappingFilesList.size != procedures.size || inheritanceFilesList.size != procedures.size) {
            throw IllegalStateException(
                "Mapping files (${mappingFilesList.size}) and inheritance files (${inheritanceFilesList.size}) " +
                        "must match the number of procedures (${procedures.size}) for action $remapAction"
            )
        }

        var shouldRemove = false
        for (i in procedures.indices) {
            val procedure = procedures[i]
            val mappingFile = mappingFilesList[i]
            val inheritanceFile = inheritanceFilesList[i]

            remap(procedure, mappingFile, inheritanceFile, fromFile, toFile)

            if (shouldRemove) {
                fromFile.delete()
            }

            if (i < procedures.size - 1) {
                fromFile = toFile
                toFile = Files.createTempFile(null, ".jar").toFile()
                shouldRemove = true
            }
        }

        targetFile.parentFile?.mkdirs()
        toFile.copyTo(targetFile, true)
        toFile.delete()
        logger.lifecycle("Successfully remapped jar (${projectName.getOrElse("unknown")}, $remapAction)")
    }

    private fun remap(
        procedure: RemapProcedure,
        mappingFile: File,
        inheritanceFile: File,
        jarFile: File,
        outputFile: File
    ) {
        Jar.init(jarFile).use { inputJar ->
            Jar.init(inheritanceFile).use { inheritanceJar ->
                val mapping = JarMapping()
                mapping.loadMappings(mappingFile.canonicalPath, procedure.reversed, false, null, null)

                val provider = JointProvider()
                provider.add(JarProvider(inputJar))
                provider.add(JarProvider(inheritanceJar))
                mapping.setFallbackInheritanceProvider(provider)

                val mapper = JarRemapper(mapping)
                mapper.remapJar(inputJar, outputFile)
            }
        }
    }

    enum class RemapAction(internal vararg val procedures: RemapProcedure) {
        MOJANG_TO_SPIGOT(RemapProcedure.MOJANG_OBF, RemapProcedure.OBF_SPIGOT),
        MOJANG_TO_OBF(RemapProcedure.MOJANG_OBF),
        OBF_TO_MOJANG(RemapProcedure.OBF_MOJANG),
        OBF_TO_SPIGOT(RemapProcedure.OBF_SPIGOT),
        SPIGOT_TO_MOJANG(RemapProcedure.SPIGOT_OBF, RemapProcedure.OBF_MOJANG),
        SPIGOT_TO_OBF(RemapProcedure.SPIGOT_OBF);
    }

    enum class RemapProcedure(
        val mappingCoord: (version: String) -> String,
        val inheritanceCoord: (version: String) -> String,
        val reversed: Boolean = false
    ) {
        MOJANG_OBF(
            { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-mojang@txt" },
            { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT:remapped-mojang" },
            true
        ),
        OBF_MOJANG(
            { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-mojang@txt" },
            { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT:remapped-obf" }
        ),
        SPIGOT_OBF(
            { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-spigot@csrg" },
            { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT" },
            true
        ),
        OBF_SPIGOT(
            { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-spigot@csrg" },
            { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT:remapped-obf" }
        );
    }
}
