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
        // config in javaagent-core uses protobuf and jackson
        // TODO relocation causes muzzle to fail! The relocation has to run before ByteBuddy plugin generates muzzle
        //  Missing method org.hypertrace.agent.config.Config$Message#getRequest()Lcom/google/protobuf/BoolValue
        relocate("com.fasterxml.jackson", "org.hypertrace.shaded.com.fasterxml.jackson")
        relocate("com.google.protobuf", "org.hypertrace.shaded.com.google.protobuf")
//        relocate("google.protobuf", "org.hypertrace.shaded.google.protobuf")
//        relocate("javax", "org.hypertrace.shaded.javax")
//        relocate("org.checkerframework", "org.hypertrace.shaded.com.checkerframework")
//        relocate("org.yaml", "org.hypertrace.shaded.org.yaml")

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
            attributes.put("Agent-Class",   "org.hypertrace.agent.HypertraceAgent")
            attributes.put("Premain-Class", "org.hypertrace.agent.HypertraceAgent")
            attributes.put("Can-Redefine-Classes", true)
            attributes.put("Can-Retransform-Classes", true)
        }
    }

    assemble {
        dependsOn(shadowJar)
    }
}
