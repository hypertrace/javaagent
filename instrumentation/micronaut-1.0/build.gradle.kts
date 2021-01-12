plugins {
    `java-library`
}

val versions: Map<String, String> by extra

val micronautVersion = "1.0.0"
val micronautTestVersion = "2.3.1"

dependencies {
    testImplementation(project(":testing-common"))
    testImplementation(project(":instrumentation:netty:netty-4.1"))

    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")

    testImplementation("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
    testImplementation("io.micronaut:micronaut-http-server-netty:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-runtime:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-inject:${micronautVersion}")

    testAnnotationProcessor("io.micronaut:micronaut-inject-java:${micronautVersion}")
}
