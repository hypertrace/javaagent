// This project is here to satisfy dependencies for the Muzzle gradle plugin (./buildSrc)

plugins {
    `java-library`
}

val versions: Map<String, String> by extra

dependencies{
    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${versions["opentelemetry_instrumentation_bom_alpha"]}"))
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
    implementation(project(":javaagent-core"))
    implementation(project(":filter-api"))
}
