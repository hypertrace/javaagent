// This project is here to satisfy dependencies for the Muzzle gradle plugin (./buildSrc)

plugins {
    `java-library`
}

dependencies{
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:0.10.1")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.10.1")
    api("io.opentelemetry:opentelemetry-api:0.10.0")
    api("io.opentelemetry:opentelemetry-sdk:0.10.0")
    api("io.opentelemetry:opentelemetry-sdk-common:0.10.0")
    api("io.opentelemetry:opentelemetry-sdk-tracing:0.10.0")
    implementation(project(":javaagent-core"))
    implementation(project(":filter-api"))
}
