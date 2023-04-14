import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream
import shadow.org.codehaus.plexus.util.IOUtil
import java.io.BufferedReader
import java.io.ByteArrayInputStream

plugins {
    id("sr.base-logic")
    id("com.github.johnrengelman.shadow")
}

val packages = mapOf<String, String>(
    "org.fusesource.jansi" to "net.skinsrestorer.shadow.jansi",
    "org.mariadb.jdbc" to "net.skinsrestorer.shadow.mariadb"
)

fun replaceSlash(text: String): String {
    return text.replace(".", "/")
}

fun replacePackagDot(text: String): String {
    var returnedText = text

    for (entry in packages.entries) {
        returnedText = returnedText.replace(entry.key, entry.value)
    }

    return returnedText;
}

fun replacePackagSlash(text: String): String {
    var returnedText = text

    for (entry in packages.entries) {
        returnedText = returnedText.replace(replaceSlash(entry.key), replaceSlash(entry.value))
    }

    return returnedText;
}

fun replaceRelocation(text: String): String {
    var returnedText = text

    returnedText = replacePackagDot(returnedText)
    returnedText = replacePackagSlash(returnedText)

    return returnedText;
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
                || pathString.contains("META-INF/native-image/jansi")
    }

    override fun transform(context: TransformerContext?) {
        val content = context?.`is`?.bufferedReader()?.use(BufferedReader::readText)!!

        val replaced = replaceRelocation(content)

        if (content.length < replaced.length) {
            replacedMap.put(context.path, replaced)
        }
    }

    override fun hasTransformedResource(): Boolean {
        return replacedMap.size > 0
    }

    override fun modifyOutputStream(os: ZipOutputStream?, preserveFileTimestamps: Boolean) {
        replacedMap.forEach { (path, value) ->
            val entry = ZipEntry(replacePackagDot(path))
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
        exclude("META-INF/SPONGEPO.SF", "META-INF/SPONGEPO.DSA", "META-INF/SPONGEPO.RSA")
        minimize() {
            exclude(dependency("org.mariadb.jdbc:mariadb-java-client"))
            exclude(dependency("org.fusesource.jansi:jansi"))
        }
        configureRelocations()
        transform(ShadowResourceTransformer())
    }

    build {
        dependsOn(shadowJar)
    }
}

fun ShadowJar.configureRelocations() {
    relocate("com.mojang.brigadier", "net.skinsrestorer.shadow.brigadier")

    relocate("com.google.gson", "net.skinsrestorer.shadow.google.gson")

    relocate("com.cryptomorin.xseries", "net.skinsrestorer.shadow.xseries")
    relocate("org.bstats", "net.skinsrestorer.shadow.bstats")
    relocate("org.fusesource.jansi", "net.skinsrestorer.shadow.jansi")

    relocate("org.mariadb.jdbc", "net.skinsrestorer.shadow.mariadb")

    relocate("org.intellij.lang.annotations", "net.skinsrestorer.shadow.ijannotations")
    relocate("org.jetbrains.annotations", "net.skinsrestorer.shadow.jbannotations")

    relocate("org.yaml.snakeyaml", "net.skinsrestorer.shadow.snakeyaml")
    relocate("net.skinsrestorer.axiom", "net.skinsrestorer.shadow.axiom")
    relocate("ch.jalu.configme", "net.skinsrestorer.shadow.configme")

    relocate("javax", "net.skinsrestorer.shadow.javax")
    relocate("ch.jalu.injector", "net.skinsrestorer.shadow.injector")

    relocate("io.papermc.lib", "net.skinsrestorer.shadow.paperlib")
    relocate("com.github.puregero.multilib", "net.skinsrestorer.shadow.multilib")
}
