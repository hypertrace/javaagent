plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("org.hypertrace.publish-maven-central-plugin")
}

val versions: Map<String, String> by extra

dependencies {
    // pin released version or snapshot with pinned version
    // update the dependencies also in the instrumentations sub-projects
    // https://oss.jfrog.org/artifactory/oss-snapshot-local/io/opentelemetry/instrumentation/auto/
    // https://dl.bintray.com/open-telemetry/maven/
    implementation("io.opentelemetry.javaagent", "opentelemetry-javaagent", version = "${versions["opentelemetry_java_agent_all"]}", classifier = "all")
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


    // TODO relocate weak map

    shadowJar {
        // config in javaagent-core uses protobuf and jackson
        // shade to the same location as OTEL, because the package prefix is used in classloader instrumentation
        // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/master/javaagent-tooling/src/main/java/io/opentelemetry/javaagent/tooling/Constants.java#L25
        // Consider changing the prefix once https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1594 is fixed.
        relocate("com.fasterxml.jackson", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.fasterxml.jackson")
        relocate("com.google", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.google")
        relocate("google.protobuf", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.google.protobuf")
        relocate("javax", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.javax")
        relocate("org.checkerframework", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.checkerframework")
        relocate("org.yaml", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.org.yaml")

        relocate("com.blogspot.mydailyjava", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.blogspot.mydailyjava")

        dependencies {
            exclude(dependency("org.codehaus.mojo:animal-sniffer-annotations"))
            exclude(dependency("javax.annotation:javax.annotation-api"))
        }

        relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")
        relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

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
