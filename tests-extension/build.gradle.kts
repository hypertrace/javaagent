plugins {
    `java-library`
}

val versions: Map<String, String> by extra

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":javaagent-bootstrap"))
    compileOnly("io.opentelemetry:opentelemetry-sdk")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetry_java_agent"]}")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${versions["opentelemetry_java_agent-tooling"]}")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0")
    annotationProcessor("com.google.auto.service:auto-service:1.0")
}
