import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.codehaus.plexus.util.IOUtil
import java.io.BufferedReader
import java.io.ByteArrayInputStream

plugins {
    id("sr.base-logic")
    id("com.gradleup.shadow")
}

val packages = mapOf(
    "org.mariadb.jdbc" to "net.skinsrestorer.shadow.mariadb"
)

fun replaceSlash(text: String): String {
    return text.replace(".", "/")
}

fun replacePackageDot(text: String): String {
    var returnedText = text

    for (entry in packages.entries) {
        returnedText = returnedText.replace(entry.key, entry.value)
    }

    return returnedText
}

fun replacePackagSlash(text: String): String {
    var returnedText = text

    for (entry in packages.entries) {
        returnedText = returnedText.replace(replaceSlash(entry.key), replaceSlash(entry.value))
    }

    return returnedText
}

fun replaceRelocation(text: String): String {
    var returnedText = text

    returnedText = replacePackageDot(returnedText)
    returnedText = replacePackagSlash(returnedText)

    return returnedText
}

class ShadowResourceTransformer : Transformer {
    @Internal
    var replacedMap: MutableMap<String, String> = HashMap()

    override fun getName(): String {
        return "ShadowResourceTransformer"
    }

    override fun canTransformResource(element: FileTreeElement?): Boolean {
        val pathString: String = element?.relativePath?.pathString!!

        return pathString.contains("META-INF/services")
    }

    override fun transform(context: TransformerContext?) {
        val content = context?.`is`?.bufferedReader()?.use(BufferedReader::readText)!!

        val replaced = replaceRelocation(content)

        if (content.length < replaced.length) {
            replacedMap[context.path] = replaced
        }
    }

    override fun hasTransformedResource(): Boolean {
        return replacedMap.isNotEmpty()
    }

    override fun modifyOutputStream(os: ZipOutputStream?, preserveFileTimestamps: Boolean) {
        replacedMap.forEach { (path, value) ->
            val entry = ZipEntry(replacePackageDot(path))
            entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
            os?.putNextEntry(entry)
            IOUtil.copy(ByteArrayInputStream(value.toByteArray()), os)
            os?.closeEntry()
        }
    }
}

tasks {
    processResources {
        expand("version" to version, "description" to description)
    }

    jar {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }

    shadowJar {
        minimize {
            exclude(dependency("org.mariadb.jdbc:mariadb-java-client"))
            exclude(project(":skinsrestorer-api"))
        }
        configureRelocations()
        transform(ShadowResourceTransformer())
    }

    build {
        dependsOn(shadowJar)
    }
}

fun ShadowJar.configureRelocations() {
    // Google inject should NOT be relocated
    relocate("com.google.gson", "net.skinsrestorer.shadow.gson")
    relocate("com.google.errorprone", "net.skinsrestorer.shadow.errorprone")

    relocate("com.cryptomorin.xseries", "net.skinsrestorer.shadow.xseries")
    relocate("org.bstats", "net.skinsrestorer.shadow.bstats")

    relocate("org.mariadb.jdbc", "net.skinsrestorer.shadow.mariadb")

    relocate("org.intellij.lang.annotations", "net.skinsrestorer.shadow.ijannotations")
    relocate("org.jetbrains.annotations", "net.skinsrestorer.shadow.jbannotations")

    relocate("org.yaml.snakeyaml", "net.skinsrestorer.shadow.snakeyaml")
    relocate("ch.jalu.configme", "net.skinsrestorer.shadow.configme")

    relocate("javax.inject", "net.skinsrestorer.shadow.javax.inject")
    relocate("javax.annotation", "net.skinsrestorer.shadow.javax.annotation")
    relocate("ch.jalu.injector", "net.skinsrestorer.shadow.injector")

    relocate("com.github.puregero.multilib", "net.skinsrestorer.shadow.multilib")

    relocate("org.incendo.cloud", "net.skinsrestorer.shadow.cloud")
    relocate("io.leangen.geantyref", "net.skinsrestorer.shadow.geantyref")
}
