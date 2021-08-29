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
//    api("io.opentelemetry.instrumentation.muzzle-check:io.opentelemetry.instrumentation.muzzle-check.gradle.plugin:0.5.0") {
//        constraints {
//            api("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:1.5.2-alpha")
//            api("io.opentelemetry.javaagent:opentelemetry-muzzle:1.5.2-alpha")
//        }
//    }
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${versions["opentelemetry_java_agent"]}")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetry_java_agent"]}")
    instrumentationMuzzle("io.opentelemetry.instrumentation.muzzle-generation:io.opentelemetry.instrumentation.muzzle-generation.gradle.plugin:0.5.0") {
        constraints {
            instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:1.5.2-alpha")
            instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-muzzle:1.5.2-alpha")
        }
    }
    instrumentationMuzzle("io.opentelemetry.instrumentation.muzzle-check:io.opentelemetry.instrumentation.muzzle-check.gradle.plugin:0.5.0") {
        constraints {
            instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:1.5.2-alpha")
            instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-muzzle:1.5.2-alpha")
        }
    }
    instrumentationMuzzle("net.bytebuddy:byte-buddy:1.11.2")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-agent:1.11.2")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0")
    instrumentationMuzzle("org.slf4j:slf4j-api:${versions["slf4j"]}")
}
