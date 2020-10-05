plugins {
    java
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

dependencies {
    implementation("io.opentelemetry.instrumentation.auto", "opentelemetry-javaagent", version = "0.8.0", classifier = "all")
}

base.archivesBaseName = "traceable-otel-javaagent"

tasks {
    compileJava {
        options.release.set(8)
    }

   // processResources {
    //    val customizationShadowTask = project(":custom").tasks.named<Jar>("shadowJar")
     //   val providerArchive = customizationShadowTask.get().archiveFile
    //    from(zipTree(providerArchive)) {
     //       into("inst")
     //       rename("(^.*)\\.class$", "$1.classdata")
    //    }
    //    dependsOn(customizationShadowTask)
   // }

    shadowJar {
        mergeServiceFiles {
            include("inst/META-INF/services/*")
        }
        exclude("**/module-info.class")
        manifest {
            attributes.put("Main-Class", "io.opentelemetry.javaagent.OpenTelemetryAgent")
            attributes.put("Agent-Class", "io.opentelemetry.javaagent.OpenTelemetryAgent")
            attributes.put("Premain-Class", "io.opentelemetry.javaagent.OpenTelemetryAgent")
            attributes.put("Can-Redefine-Classes", "true")
            attributes.put("Can-Retransform-Classes", "true")
            attributes.put("Implementation-Vendor", "Traceable.ai")
            attributes.put("Implementation-Version", project.version)
            // TODO extract OTEL version to a property
            attributes.put("OpenTelemetry-Instrumentation-Version", "0.8.0")
            // TODO change to repository URL
            attributes.put("Implementation-Url", "https://traceable.ai")
        }
    }

    assemble {
        dependsOn(shadowJar)
    }
}
