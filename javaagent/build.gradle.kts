plugins {
    java
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

dependencies {
    // pin released version or snapshot with pinned version
    // update the dependencies also in the instrumentations sub-projects
    // https://oss.jfrog.org/artifactory/oss-snapshot-local/io/opentelemetry/instrumentation/auto/
    implementation("io.opentelemetry.instrumentation.auto", "opentelemetry-javaagent", version = "0.9.0-20201008.091003-73", classifier = "all")
    implementation("ai.traceable.agent", "agent-config", version = "0.2.4")
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
        relocate("com.fasterxml.jackson", "org.hypertrace.shaded.com.fasterxml.jackson")
        mergeServiceFiles {
            include("inst/META-INF/services/*")
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
