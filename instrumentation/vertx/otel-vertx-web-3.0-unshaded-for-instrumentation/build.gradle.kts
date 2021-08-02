import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import shadow.org.apache.tools.zip.ZipOutputStream
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import shadow.org.apache.tools.zip.ZipEntry

plugins {
    id("io.opentelemetry.instrumentation.un-shade")
}

val versions: Map<String, String> by extra

dependencies {
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-vertx-web-3.0:${versions["opentelemetry_java_agent"]}:all")
}

tasks.shadowJar {
    // a special case exists here. this module shades the vertx and JDBC modules into it, which makes it hard to consume.
    // therefore, we exclude those shaded modules and remove their META-INF/services entry to prevent
    // them from being consumed by downstream tooling (like muzzle) and confusing it for a violation
    exclude("**/shaded/**/netty/**")
    exclude("**/javaagent/instrumentation/netty/**")
    exclude("**/javaagent/instrumentation/jdbc/**")
    transform(ExcludeServiceTransformer::class.java) {
        excludePackage("netty")
        excludePackage("jdbc")
    }
}

@com.github.jengelman.gradle.plugins.shadow.transformers.CacheableTransformer
class ExcludeServiceTransformer : com.github.jengelman.gradle.plugins.shadow.transformers.Transformer {
    private val patternSet = PatternSet().include("META-INF/services/**")
    private val serviceEntries: MutableMap<String, ServiceFileTransformer.ServiceStream> = mutableMapOf()
    private val excludedPackages: MutableList<String> = mutableListOf()

    fun excludePackage(p: String) {
        excludedPackages.add(p)
    }

    override fun canTransformResource(p0: FileTreeElement?): Boolean {
        return if (p0 is com.github.jengelman.gradle.plugins.shadow.tasks.ShadowCopyAction.ArchiveFileTreeElement) {
            patternSet.asSpec.isSatisfiedBy(p0.asFileTreeElement())
        } else {
            patternSet.asSpec.isSatisfiedBy(p0)
        }
    }

    override fun transform(p0: TransformerContext) {
        BufferedReader(p0.`is`.reader()).use {
            it.readLines().forEach { element ->
                if (!excludedPackages.any { excludedPackage -> element.contains(excludedPackage) }) {
                    val serviceStream = serviceEntries.computeIfAbsent(p0.path) {
                        ServiceFileTransformer.ServiceStream()
                    }
                    serviceStream.append(ByteArrayInputStream(element.toByteArray()))
                }
            }
        }
    }

    override fun hasTransformedResource(): Boolean {
        return serviceEntries.isNotEmpty()
    }

    override fun modifyOutputStream(p0: ZipOutputStream, p1: Boolean) {
        serviceEntries.forEach { (path, stream) ->
            val entry = ZipEntry(path)
            entry.time = TransformerContext.getEntryTimestamp(p1, entry.time)
            p0.putNextEntry(entry)
            org.codehaus.plexus.util.IOUtil.copy(stream.toInputStream(), p0)
            p0.closeEntry()
        }
    }
}
