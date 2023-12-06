plugins {
    `java-library`
    id("org.hypertrace.publish-maven-central-plugin")
}

val versions: Map<String, String> by extra

dependencies {
    api("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${versions["opentelemetry"]}")
    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0")
}
