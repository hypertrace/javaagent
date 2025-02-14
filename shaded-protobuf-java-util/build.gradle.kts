plugins {
    `java-library`
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation("com.google.protobuf:protobuf-java-util:3.25.5")

    // fix vulnerability
    constraints {
        implementation("com.google.code.gson:gson:2.8.9")
    }
}

tasks.shadowJar {
    relocate("com.google.protobuf", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf")
    relocate("com.google.protobuf.util", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.util")
    relocate("com.google.gson", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.gson")
    relocate("com.google.common", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.common") // Add this
    relocate("com.google.guava", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.guava")
}
