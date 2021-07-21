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
//    relocate("com.google.common", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.common")
//    relocate("com.google.errorprone", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.errorprone")
//    relocate("com.google.thirdparty", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.thirdparty")


//    relocate("com.google", "io.opentelemetry.javaagent.instrumentation.hypertrace.com.google")

}
