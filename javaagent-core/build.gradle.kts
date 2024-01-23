plugins {
    `java-library`
    id("org.hypertrace.publish-maven-central-plugin")
}

val versions: Map<String, String> by extra

dependencies {
    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${versions["opentelemetry_instrumentation_bom_alpha"]}"))
    api("io.opentelemetry:opentelemetry-api")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0")
}
