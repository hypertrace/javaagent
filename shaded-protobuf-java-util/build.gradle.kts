plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

dependencies {
    implementation("com.google.protobuf:protobuf-java-util:3.4.0") {
        exclude("com.google.protobuf", "protobuf-java")
        exclude("com.google.guava", "guava")
    }
}

tasks.shadowJar {
    relocate("com.google.protobuf.util", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.util")
    relocate("com.google.gson", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.gson")
}
