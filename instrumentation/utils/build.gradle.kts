plugins {
    `java-library`
    id("io.opentelemetry.instrumentation.auto-instrumentation")
}

val versions: Map<String, String> by extra

dependencies {
    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
}
