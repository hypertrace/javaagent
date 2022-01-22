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
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-muzzle:${versions["opentelemetry_java_agent"]}")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-instrumentation-api:${versions["opentelemetry_java_agent"]}")
    instrumentationMuzzle("io.opentelemetry.instrumentation:gradle-plugins:${versions["opentelemetry_gradle_plugin"]}")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${versions["opentelemetry_java_agent"]}")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetry_java_agent"]}")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-dep:1.12.6")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0")
    instrumentationMuzzle("org.slf4j:slf4j-api:${versions["slf4j"]}")
}
