plugins {
    id("com.github.johnrengelman.shadow")
}

val versions: Map<String, String> by extra

dependencies {
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}:all")
}

tasks.shadowJar {
    relocate("io.opentelemetry.javaagent.shaded.io.opentelemetry.api", "io.opentelemetry.api")
    relocate("io.opentelemetry.javaagent.shaded.io.opentelemetry.context", "io.opentelemetry.context")
    relocate("io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv", "io.opentelemetry.semconv")
    relocate("io.opentelemetry.javaagent.shaded.instrumentation", "io.opentelemetry.instrumentation")
}