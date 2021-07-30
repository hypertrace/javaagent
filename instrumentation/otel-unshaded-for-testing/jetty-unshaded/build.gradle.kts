plugins {
    id("io.opentelemetry.instrumentation.un-shade")
}

val versions: Map<String, String> by extra

dependencies {
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jetty-8.0:${versions["opentelemetry_java_agent"]}:all")
}