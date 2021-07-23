plugins {
    id("io.opentelemetry.instrumentation.un-shade")
}

val versions: Map<String, String> by extra

dependencies {
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}:all")
}