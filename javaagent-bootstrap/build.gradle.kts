// This project is here to satisfy dependencies for the Muzzle gradle plugin (./buildSrc)

plugins {
    java
}

dependencies{
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-bootstrap:0.9.0-20201009.192532-82")
    implementation("io.opentelemetry.instrumentation:opentelemetry-auto-api:0.9.0-20201009.192531-82")
}
