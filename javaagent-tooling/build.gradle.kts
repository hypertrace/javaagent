// This project is here to satisfy dependencies for the Muzzle gradle plugin (./buildSrc)

plugins {
    java
}

dependencies{
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-tooling:0.9.0-20201009.101126-80")
    implementation("io.opentelemetry.instrumentation:opentelemetry-auto-api:0.9.0-20201009.192531-82")
}

