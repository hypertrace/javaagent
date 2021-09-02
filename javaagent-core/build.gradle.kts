plugins {
    `java-library`
    id("org.hypertrace.publish-maven-central-plugin")
}

val versions: Map<String, String> by extra

dependencies {
    api("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-instrumentation-api:${versions["opentelemetry_java_agent"]}")
    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
}
