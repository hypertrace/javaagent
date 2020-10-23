// This project is here to satisfy dependencies for the Muzzle gradle plugin (./buildSrc)

plugins {
    `java-library`
}

dependencies{
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:0.9.0")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.9.0")
}
