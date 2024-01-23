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
    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${versions["opentelemetry_instrumentation_bom_alpha"]}"))
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-muzzle")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
    instrumentationMuzzle("io.opentelemetry.instrumentation:gradle-plugins:${versions["opentelemetry_gradle_plugin"]}") {
        exclude(group = "gradle.plugin.com.github.johnrengelman", module = "shadow")
    }
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-dep:1.12.6")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0")
    instrumentationMuzzle("org.slf4j:slf4j-api:${versions["slf4j"]}")
}
