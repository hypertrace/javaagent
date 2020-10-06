plugins {
    java
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

dependencies {
    compileOnly("javax.servlet:javax.servlet-api:3.0.1")
    compile("org.slf4j:slf4j-api:1.7.30")
    compile("net.bytebuddy:byte-buddy:1.10.10")
    compile("com.google.auto.service:auto-service:1.0-rc7")
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")

    compile("io.opentelemetry.instrumentation.auto:opentelemetry-auto-servlet-3.0:0.8.0")

    implementation("io.opentelemetry:opentelemetry-sdk:0.8.0")
    //io/opentelemetry/instrumentation/auto/opentelemetry-auto-javaagent-exporters-otlp
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-exporters-otlp:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-exporters-jaeger:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-exporters-zipkin:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-exporters-logging:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-tooling:0.8.0")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-bootstrap:0.8.0")
}

tasks {
    compileJava {
        options.release.set(8)
    }

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

        // relocate OpenTelemetry API dependency
        relocate("io.grpc", "io.opentelemetry.javaagent.shaded.io.grpc")
    }
}