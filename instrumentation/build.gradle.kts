plugins {
    id("com.github.johnrengelman.shadow")
    `java-library`
}

dependencies{
    implementation(project(":instrumentation:servlet:servlet-rw"))
    implementation(project(":instrumentation:servlet:servlet-3.0"))
    implementation(project(":instrumentation:spark-2.3"))
    implementation(project(":instrumentation:grpc-1.6"))
    implementation(project(":instrumentation:grpc-shaded-netty-1.9"))
    implementation(project(":instrumentation:okhttp:okhttp-3.0"))
    implementation(project(":instrumentation:apache-httpclient-4.0"))
    implementation(project(":instrumentation:jaxrs-client-2.0"))
    implementation(project(":instrumentation:java-streams"))
    implementation(project(":instrumentation:apache-httpasyncclient-4.1"))
    implementation(project(":instrumentation:netty:netty-4.0"))
    implementation(project(":instrumentation:netty:netty-4.1"))
    implementation(project(":instrumentation:undertow:undertow-1.4"))
    implementation(project(":instrumentation:undertow:undertow-servlet-1.4"))
    implementation(project(":otel-extensions"))
}

tasks {
    // Keep in sync with https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/f893ca540b72a895fbf18c14d2df8d1cabaf2c7f/instrumentation/instrumentation.gradle#L51
    shadowJar {
        dependencies{
            // exclude core, it lives in the bootstrap classloader
            exclude(project(":javaagent-core"))
        }

        mergeServiceFiles()

        relocate("com.blogspot.mydailyjava.weaklockfree", "io.opentelemetry.instrumentation.api.internal.shaded.weaklockfree")

        exclude("**/module-info.class")

        relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")
        relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

//        // prevents conflict with library instrumentation
        relocate("io.opentelemetry.instrumentation.api", "io.opentelemetry.javaagent.shaded.instrumentation.api")
        //opentelemetry rewrite library instrumentation dependencies
        relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
            exclude("io.opentelemetry.javaagent.instrumentation.**")
        }

        // relocate OpenTelemetry API
        relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
        relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
        relocate("io.opentelemetry.spi", "io.opentelemetry.javaagent.shaded.io.opentelemetry.spi")
        relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
        relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")
        relocate("io.opentelemetry.extension.aws", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")
    }
}
