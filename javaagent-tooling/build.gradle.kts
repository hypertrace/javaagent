// This project is here to satisfy dependencies for the Muzzle gradle plugin (./buildSrc)

plugins {
    `java-library`
}

val instrumentationMuzzle by configurations.creating {
    extendsFrom(configurations.implementation.get())
    extendsFrom(configurations.api.get())
}

dependencies {
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:0.11.0")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.11.0")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:0.11.0")
    instrumentationMuzzle("net.bytebuddy:byte-buddy:1.10.10")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-agent:1.10.10")
    instrumentationMuzzle("com.blogspot.mydailyjava:weak-lock-free:0.15")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0-rc7")
    instrumentationMuzzle("org.slf4j:slf4j-api:1.7.30")
}
