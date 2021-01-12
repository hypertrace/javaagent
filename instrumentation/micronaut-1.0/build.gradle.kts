plugins {
    `java-library`
}

val versions: Map<String, String> by extra

//version += mapOf<String, String>("micronaut_version" to "1.0.0")

dependencies {
    testImplementation(project(":testing-common"))
    testImplementation(project(":instrumentation:netty:netty-4.1"))

    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")

    testImplementation("io.micronaut.test:micronaut-test-junit5:1.1.0")
    testImplementation("io.micronaut:micronaut-http-server-netty:1.1.0")
    testImplementation("io.micronaut:micronaut-http-client:1.1.0")
    testImplementation("io.micronaut:micronaut-runtime:1.1.0")
//    testImplementation("io.micronaut:micronaut-validation:1.1.0")
    testImplementation("io.micronaut:micronaut-inject:1.1.0")

    testAnnotationProcessor("io.micronaut:micronaut-inject-java:1.1.0")
//    testAnnotationProcessor("io.micronaut:micronaut-validation:1.1.0")
}
