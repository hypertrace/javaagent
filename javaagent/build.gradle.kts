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

// Step 1: Extract instrumentation project's shadowJar into inst/ folder
tasks.register<Copy>("extractCustomInstrumentationToInst") {
    description = "Extracts instrumentation project's shadowJar into inst/ folder"

    val customizationShadowTask = project(":instrumentation").tasks.named<Jar>("shadowJar")
    val providerArchive = customizationShadowTask.get().archiveFile

    from(zipTree(providerArchive)) {
        into("inst")
        rename("(^.*)\\.class$", "$1.classdata")
    }

    into("$buildDir/resources/main")

    exclude("**/META-INF/LICENSE")
    dependsOn(customizationShadowTask)
}

// Step 2: Extract OpenTelemetry Java Agent's inst/ files and rename .classdata to .class
tasks.register<Copy>("extractOtelAgentJarInstClassdata") {
    description = "Extracts OpenTelemetry Java Agent's .classdata files and renames them to .class"

    val otelJavaAgentJar = configurations.compileClasspath.get()
        .filter { it.name.contains("opentelemetry-javaagent") }
        .singleOrNull() ?: throw GradleException("OpenTelemetry Java Agent JAR not found")

    doFirst {
        println("OpenTelemetry Java Agent JAR: $otelJavaAgentJar")
    }

    from(zipTree(otelJavaAgentJar)) {
        include("inst/**")
        rename("(^.*)\\.classdata$", "$1.class")
    }

    // Output to a temporary directory
    into("$buildDir/tmp/otel-classdata-for-relocation")
}

// Step 3: Move contents to inst/ folder with relocated paths
tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("relocateOtelClassesToInst") {
    description = "Relocates OpenTelemetry classes to inst/ folder with ai.traceable prefix"

    dependsOn("extractOtelAgentJarInstClassdata")

    from("$buildDir/tmp/otel-classdata-for-relocation/inst")

    destinationDirectory.set(file("$buildDir/tmp/relocated-otel-classdata"))
    archiveFileName.set("relocated-otel-classdata.jar")

    relocate("io.opentelemetry", "ai.traceable.io.opentelemetry")

    eachFile {
        path = "inst/ai/traceable/$path"
    }
}

// Step 3b: Extract the relocated JAR
tasks.register<Copy>("extractRelocatedOtelClasses") {
    description = "Extracts relocated OpenTelemetry classes"

    dependsOn("relocateOtelClassesToInst")

    from(zipTree("$buildDir/tmp/relocated-otel-classdata/relocated-otel-classdata.jar"))
    into("$buildDir/tmp/relocated-otel-classes")
}

tasks.register("extractOtelInstrumentationToInst") {
    description = "Removes empty directories from the relocated classes directory"

    dependsOn("extractRelocatedOtelClasses")

    doLast {
        // Find and delete empty directories
        val instDir = file("$buildDir/tmp/relocated-otel-classes")
        if (instDir.exists()) {
            deleteEmptyDirs(instDir)
        }
    }
}

// Helper function to recursively delete empty directories
fun deleteEmptyDirs(dir: File) {
    if (!dir.isDirectory) return

    val children = dir.listFiles() ?: return

    // Recursively process subdirectories
    children.filter { it.isDirectory }.forEach { deleteEmptyDirs(it) }

    // Check if directory is empty after processing subdirectories
    if (dir.listFiles()?.isEmpty() == true) {
        dir.delete()
    }
}

// Step 4: Convert all .class files to .classdata and combine with instrumentation files
tasks.register<Copy>("combineAndConvertToClassdata") {
    description = "Combines all classes and converts to .classdata"

    dependsOn("extractCustomInstrumentationToInst", "extractOtelInstrumentationToInst")

    // include the relocated OpenTelemetry classes
    from("$buildDir/tmp/relocated-otel-classes") {
        rename("(^.*)\\.class$", "$1.classdata")
    }

    // Output to the resources directory for inclusion in the final JAR
    into("$buildDir/resources/main")

    // If there are conflicts, our instrumentation project files win
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Modify the existing processResources task to depend on our new task
tasks.named<ProcessResources>("processResources") {
    dependsOn("combineAndConvertToClassdata")
    exclude("**/META-INF/LICENSE")
}

tasks {

    shadowJar {
        relocate("com.blogspot.mydailyjava.weaklockfree", "ai.traceable.io.opentelemetry.instrumentation.api.internal.shaded.weaklockfree")

        dependencies {
            exclude(dependency("org.codehaus.mojo:animal-sniffer-annotations"))
            exclude(dependency("javax.annotation:javax.annotation-api"))
        }

        relocate("org.slf4j", "ai.traceable.io.opentelemetry.javaagent.slf4j")
        relocate("java.util.logging.Logger", "ai.traceable.io.opentelemetry.javaagent.bootstrap.PatchLogger")
        relocate("com.fasterxml.jackson", "ai.traceable.io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.fasterxml.jackson")
        relocate("org.yaml", "ai.traceable.io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.org.yaml")

        // prevents conflict with library instrumentation
        relocate("io.opentelemetry.instrumentation.api", "ai.traceable.io.opentelemetry.javaagent.shaded.instrumentation.api")

        // relocate OpenTelemetry API
        relocate("io.opentelemetry.api", "ai.traceable.io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
        relocate("io.opentelemetry.semconv", "ai.traceable.io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
        relocate("io.opentelemetry.spi", "ai.traceable.io.opentelemetry.javaagent.shaded.io.opentelemetry.spi")
        relocate("io.opentelemetry.context", "ai.traceable.io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
        relocate("io.opentelemetry.extension.kotlin", "ai.traceable.io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")
        relocate("io.opentelemetry.extension.aws", "ai.traceable.io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")
        // Shade everything else of io.opentelemetry into ai.traceable.io.opentelemetry
        relocate("io.opentelemetry", "ai.traceable.io.opentelemetry")

        mergeServiceFiles {
            include("inst/META-INF/services/*")
            // exclude because it would be shaded twice and the META-INF/services/ would be io.opentelemetry.javaagent.shaded.io.grpc
            exclude("inst/META-INF/services/io.grpc*")
        }
        // Fix CVE-2024-7254, opentelemetry-javaagent brings in io.prometheus.metrics which uses deps of high vulnerability protobuf-java version
        // This was fixed in 2.x.x versions of opentelemetry-javaagent(which needs us to upgrade from 1.33.0)
        // TODO: Remove this exclusion after otel-javaagent upgrade which has CVE-2024-7254 fix
        exclude("inst/ai/traceable/io/prometheus/metrics/shaded/com_google_protobuf_3_21_7/**")
        exclude("**/module-info.class")
        manifest {
            attributes.put("Implementation-Title", "javaagent")
            attributes.put("Implementation-Version", project.version)
            attributes.put("OpenTelemetry-Instrumentation-Version", "${versions["opentelemetry_java_agent"]}")
            attributes.put("Implementation-Vendor", "Hypertrace.org")
            attributes.put("Implementation-Url", "https://github.com/hypertrace/javaagent")
            attributes.put("Main-Class", "ai.traceable.io.opentelemetry.javaagent.OpenTelemetryAgent")
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
