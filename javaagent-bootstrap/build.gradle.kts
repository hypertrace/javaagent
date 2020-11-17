// This project is here to satisfy dependencies for the Muzzle gradle plugin (./buildSrc)

plugins {
    `java-library`
}

dependencies{
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:0.10.1")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.10.1")
    implementation(project(":javaagent-core"))
    implementation(project(":filter-api"))
}
