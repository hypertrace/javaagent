plugins {
    id("com.github.johnrengelman.shadow") version "6.0.0"
    `java-library`
}

subprojects {
    dependencies {
        implementation("org.slf4j:slf4j-api:1.7.30")
        implementation("com.google.auto.service:auto-service:1.0-rc7")
        annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")

        implementation("io.opentelemetry:opentelemetry-api:0.9.1")
        implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:0.9.0")
        implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:0.9.0")
        implementation(project(":javaagent-core"))
        implementation(project(":blocking"))
    }

    // This ensures to build jars for all dependencies in instrumentation module for ByteBuddy
    configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME) {
        attributes {
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named<LibraryElements>(LibraryElements.JAR))
        }
    }
}

dependencies{
    implementation(project(":instrumentation:servlet:servlet-common"))
    implementation(project(":instrumentation:servlet:servlet-2.3"))
    implementation(project(":instrumentation:servlet:servlet-3.0"))
    implementation(project(":instrumentation:servlet:servlet-3.1"))
    implementation(project(":instrumentation:spark-web-framework-2.3"))
    implementation(project(":instrumentation:grpc-1.5"))
    implementation(project(":instrumentation:okhttp:okhttp-3.0"))
}

tasks {
    // Keep in sync with https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/f893ca540b72a895fbf18c14d2df8d1cabaf2c7f/instrumentation/instrumentation.gradle#L51
    shadowJar {
        mergeServiceFiles()

        exclude("**/module-info.class")

        // Prevents conflict with other SLF4J instances. Important for premain.
        relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")
        // rewrite dependencies calling Logger.getLogger
        relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

        // prevents conflict with library instrumentation
        relocate("io.opentelemetry.instrumentation.api", "io.opentelemetry.javaagent.shaded.instrumentation.api")

        // relocate OpenTelemetry API
        relocate("io.opentelemetry.OpenTelemetry", "io.opentelemetry.javaagent.shaded.io.opentelemetry.OpenTelemetry")
        relocate("io.opentelemetry.common", "io.opentelemetry.javaagent.shaded.io.opentelemetry.common")
        relocate("io.opentelemetry.baggage", "io.opentelemetry.javaagent.shaded.io.opentelemetry.baggage")
        relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
        relocate("io.opentelemetry.internal", "io.opentelemetry.javaagent.shaded.io.opentelemetry.internal")
        relocate("io.opentelemetry.metrics", "io.opentelemetry.javaagent.shaded.io.opentelemetry.metrics")
        relocate("io.opentelemetry.trace", "io.opentelemetry.javaagent.shaded.io.opentelemetry.trace")

        //opentelemetry rewrite library instrumentation dependencies
        relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
            exclude("io.opentelemetry.instrumentation.auto.**")
            exclude("io.opentelemetry.instrumentation.hypertrace.**")
        }

        // relocate OpenTelemetry API dependency
        relocate("io.grpc", "io.opentelemetry.javaagent.shaded.io.grpc")
    }
}
