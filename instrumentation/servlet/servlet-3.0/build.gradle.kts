plugins {
    java
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

dependencies {
    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    implementation("com.google.auto.service:auto-service:1.0-rc7")
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")

    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-auto-servlet-3.0:0.8.0")

    implementation("io.opentelemetry:opentelemetry-sdk:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-exporters-otlp:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-exporters-jaeger:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-exporters-zipkin:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-exporters-logging:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-tooling:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-bootstrap:0.8.0")
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
        relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
        relocate("io.opentelemetry.correlationcontext", "io.opentelemetry.javaagent.shaded.io.opentelemetry.correlationcontext")
        relocate("io.opentelemetry.internal", "io.opentelemetry.javaagent.shaded.io.opentelemetry.internal")
        relocate("io.opentelemetry.metrics", "io.opentelemetry.javaagent.shaded.io.opentelemetry.metrics")
        relocate("io.opentelemetry.trace", "io.opentelemetry.javaagent.shaded.io.opentelemetry.trace")

        // rewrite library instrumentation dependencies
        relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
            exclude("io.opentelemetry.instrumentation.auto.**")
        }

        // relocate OpenTelemetry API dependency
        relocate("io.grpc", "io.opentelemetry.javaagent.shaded.io.grpc")
    }
}
