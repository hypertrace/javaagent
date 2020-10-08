plugins {
    java
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

dependencies {
    implementation("io.opentelemetry.instrumentation.auto", "opentelemetry-javaagent", version = "0.8.0", classifier = "all")
}

base.archivesBaseName = "traceable-otel-javaagent"

tasks {
    processResources {
        val customizationShadowTask = project(":instrumentation:servlet:servlet-3.0").tasks.named<Jar>("shadowJar")
        val providerArchive = customizationShadowTask.get().archiveFile
        from(zipTree(providerArchive)) {
            into("inst")
            rename("(^.*)\\.class$", "$1.classdata")
        }
        dependsOn(customizationShadowTask)
    }

    shadowJar {
        mergeServiceFiles {
            include("inst/META-INF/services/*")
        }
        // TODO we could order the Instrumenter services to guarantee that traceable instrumentation runs after OTEL
//        transform(com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer())
        exclude("**/module-info.class")
        manifest {
            attributes.put("Implementation-Title", "javaagent")
            attributes.put("Implementation-Version", project.version)
            // TODO set version from a property
            attributes.put("OpenTelemetry-Instrumentation-Version", "0.8.0")
            attributes.put("Implementation-Vendor", "Traceable.ai")
            // TODO set to Github repository URL
            attributes.put("Implementation-Url", "https://traceable.ai")
            attributes.put("Main-Class",    "io.opentelemetry.javaagent.OpenTelemetryAgent")
            attributes.put("Agent-Class",   "io.opentelemetry.javaagent.OpenTelemetryAgent")
            attributes.put("Premain-Class", "io.opentelemetry.javaagent.OpenTelemetryAgent")
            attributes.put("Can-Redefine-Classes", true)
            attributes.put("Can-Retransform-Classes", true)
        }
    }

    assemble {
        dependsOn(shadowJar)
    }
}
