// This project is here to satisfy dependencies for the Muzzle gradle plugin (./buildSrc)

plugins {
    `java-library`
}

val instrumentationMuzzle by configurations.creating {
    extendsFrom(configurations.implementation.get())
    extendsFrom(configurations.api.get())
}

val versions: Map<String, String> by extra

dependencies {
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${versions["opentelemetry_java_agent"]}")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-api:${versions["opentelemetry_java_agent"]}")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${versions["opentelemetry_java_agent"]}")
    instrumentationMuzzle("net.bytebuddy:byte-buddy:1.10.18")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-agent:1.10.18")
    instrumentationMuzzle("com.blogspot.mydailyjava:weak-lock-free:0.15")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0-rc7")
    instrumentationMuzzle("org.slf4j:slf4j-api:1.7.30")
}
