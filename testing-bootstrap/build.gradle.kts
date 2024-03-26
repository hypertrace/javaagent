plugins {
    `java-library`
    id("com.github.johnrengelman.shadow")
}

// Depend on all libraries that are in the bootstrap classloader when running the agent. When
// running tests, we simulate this by adding the jar produced by this project to the bootstrap
// classpath.

val versions: Map<String, String> by extra

dependencies {
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${versions["opentelemetry_java_agent"]}")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetry_java_agent"]}")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${versions["opentelemetry"]}")
    implementation(project(":javaagent-core"))
    implementation(project(":filter-api"))

    implementation("ch.qos.logback:logback-classic:1.4.6")
    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
}
