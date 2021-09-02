// This project is here to satisfy dependencies for the Muzzle gradle plugin (./buildSrc)

plugins {
    `java-library`
}

val instrumentationMuzzle by configurations.creating {
    extendsFrom(configurations.implementation.get())
    extendsFrom(configurations.api.get())
}

val versions: Map<String, String> by extra

repositories {
    gradlePluginPortal()
}

dependencies {
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${versions["opentelemetry_java_agent"]}")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-instrumentation-api:${versions["opentelemetry_java_agent"]}")
    instrumentationMuzzle("io.opentelemetry.instrumentation:gradle-plugins:0.7.0")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${versions["opentelemetry_java_agent"]}")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetry_java_agent"]}")
    instrumentationMuzzle("net.bytebuddy:byte-buddy:1.11.2")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-agent:1.11.2")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0")
    instrumentationMuzzle("org.slf4j:slf4j-api:${versions["slf4j"]}")
}
