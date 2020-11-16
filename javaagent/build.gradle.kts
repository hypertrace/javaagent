plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("org.hypertrace.publish-plugin")
}

dependencies {
    // pin released version or snapshot with pinned version
    // update the dependencies also in the instrumentations sub-projects
    // https://oss.jfrog.org/artifactory/oss-snapshot-local/io/opentelemetry/instrumentation/auto/
    // https://dl.bintray.com/open-telemetry/maven/
    implementation("io.opentelemetry.javaagent", "opentelemetry-javaagent", version = "0.9.0", classifier = "all")
    implementation(project(":javaagent-core"))
    implementation(project(":filter"))
    implementation(project(":filter-custom-opa"))
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
        dependsOn(customizationShadowTask)
    }


    shadowJar {
        // shade to match with the package prefix for the classloader instrumentation
        relocate("org.hypertrace.agent", "io.opentelemetry.javaagent.shaded.org.hypertrace.agent") {
            exclude("org.hypertrace.agent.instrument.*")
        }

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

        // used by the filter-custom-opa
        relocate("okhttp3", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.okhttp3")
        relocate("okio", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.okio")

        dependencies {
            exclude(dependency("org.codehaus.mojo:animal-sniffer-annotations"))
            exclude(dependency("javax.annotation:javax.annotation-api"))
        }

        // relocate following classes because javaagent-core uses OTEL APIs
        relocate("io.grpc", "io.opentelemetry.javaagent.shaded.io.grpc")
        relocate("io.opentelemetry.OpenTelemetry", "io.opentelemetry.javaagent.shaded.io.opentelemetry.OpenTelemetry")
        relocate("io.opentelemetry.common", "io.opentelemetry.javaagent.shaded.io.opentelemetry.common")
        relocate("io.opentelemetry.baggage", "io.opentelemetry.javaagent.shaded.io.opentelemetry.baggage")
        relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
        relocate("io.opentelemetry.internal", "io.opentelemetry.javaagent.shaded.io.opentelemetry.internal")
        relocate("io.opentelemetry.metrics", "io.opentelemetry.javaagent.shaded.io.opentelemetry.metrics")
        relocate("io.opentelemetry.trace", "io.opentelemetry.javaagent.shaded.io.opentelemetry.trace")

        relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")

        mergeServiceFiles {
            include("inst/META-INF/services/*")
            // exclude because it would be shaded twice and the META-INF/services/ would be io.opentelemetry.javaagent.shaded.io.grpc
            exclude("inst/META-INF/services/io.grpc*")
        }
        exclude("**/module-info.class")
        manifest {
            attributes.put("Implementation-Title", "javaagent")
            attributes.put("Implementation-Version", project.version)
            // TODO set version from a property
            attributes.put("OpenTelemetry-Instrumentation-Version", "0.8.0")
            attributes.put("Implementation-Vendor", "Hypertrace.org")
            // TODO set to Github repository URL
            attributes.put("Implementation-Url", "https://hypertrace.org")
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
