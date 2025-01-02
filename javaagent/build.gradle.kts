plugins {
    `java-library`
    id("com.github.johnrengelman.shadow")
    id("org.hypertrace.publish-maven-central-plugin")
}

val versions: Map<String, String> by extra

dependencies {
    // pin released version or snapshot with pinned version
    // update the dependencies also in the instrumentations sub-projects
    // https://oss.jfrog.org/artifactory/oss-snapshot-local/io/opentelemetry/instrumentation/auto/
    // https://dl.bintray.com/open-telemetry/maven/
    implementation("io.opentelemetry.javaagent", "opentelemetry-javaagent", version = "${versions["opentelemetry_java_agent_all"]}")
    implementation(project(":filter-api"))
}

base.archivesBaseName = "hypertrace-agent"

tasks {
    processResources {
        val customizationShadowTask = project(":instrumentation").tasks.named<Jar>("shadowJar")
        val providerArchive = customizationShadowTask.get().archiveFile
        from(zipTree(providerArchive)) {
            into("inst")
            rename("(^.*)\\.class$", "$1.classdata")
        }
        exclude("**/META-INF/LICENSE")
        dependsOn(customizationShadowTask)
    }

    shadowJar {
        relocate("com.blogspot.mydailyjava.weaklockfree", "io.opentelemetry.instrumentation.api.internal.shaded.weaklockfree")

        dependencies {
            exclude(dependency("org.codehaus.mojo:animal-sniffer-annotations"))
            exclude(dependency("javax.annotation:javax.annotation-api"))
        }

        relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")
        relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")
        relocate("com.fasterxml.jackson", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.fasterxml.jackson")
        relocate("org.yaml", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.org.yaml")

        // prevents conflict with library instrumentation
        relocate("io.opentelemetry.instrumentation.api", "io.opentelemetry.javaagent.shaded.instrumentation.api")

        // relocate OpenTelemetry API
        relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
        relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
        relocate("io.opentelemetry.spi", "io.opentelemetry.javaagent.shaded.io.opentelemetry.spi")
        relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
        relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")
        relocate("io.opentelemetry.extension.aws", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")

        mergeServiceFiles {
            include("inst/META-INF/services/*")
            // exclude because it would be shaded twice and the META-INF/services/ would be io.opentelemetry.javaagent.shaded.io.grpc
            exclude("inst/META-INF/services/io.grpc*")
        }
        // Fix CVE-2024-7254, opentelemetry-javaagent brings in io.prometheus.metrics which uses deps of high vulnerability protobuf-java version
        // This was fixed in 2.x.x versions of opentelemetry-javaagent(which needs us to upgrade from 1.33.0)
        exclude("inst/io/prometheus/metrics/shaded/com_google_protobuf_3_21_7/**")
        exclude("**/module-info.class")
        manifest {
            attributes.put("Implementation-Title", "javaagent")
            attributes.put("Implementation-Version", project.version)
            attributes.put("OpenTelemetry-Instrumentation-Version", "${versions["opentelemetry_java_agent"]}")
            attributes.put("Implementation-Vendor", "Hypertrace.org")
            attributes.put("Implementation-Url", "https://github.com/hypertrace/javaagent")
            attributes.put("Main-Class", "io.opentelemetry.javaagent.OpenTelemetryAgent")
            attributes.put("Agent-Class",   "org.hypertrace.agent.instrument.HypertraceAgent")
            attributes.put("Premain-Class", "org.hypertrace.agent.instrument.HypertraceAgent")
            attributes.put("Can-Redefine-Classes", true)
            attributes.put("Can-Retransform-Classes", true)
        }
    }

    assemble {
        dependsOn(shadowJar)
    }
}
