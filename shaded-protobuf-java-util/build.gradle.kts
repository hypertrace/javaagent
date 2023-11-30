plugins {
    `java-library`
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation("com.google.protobuf:protobuf-java-util:3.4.0") {
        exclude("com.google.protobuf", "protobuf-java")
        exclude("com.google.guava", "guava")
    }
    // fix vulnerability
    constraints {
        implementation("com.google.code.gson:gson:2.8.9")
    }
}

tasks.shadowJar {
    relocate("com.google.protobuf.util", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.util")
    relocate("com.google.gson", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.gson")
}
