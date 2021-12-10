plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
}

val versions: Map<String, String> by extra

val micronautVersion = "3.2.0"
val micronautTestVersion = "3.0.5"

dependencies {
    implementation(project(":instrumentation:netty:netty-4.1"))
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")
    testImplementation(testFixtures(project(":testing-common")))
    testImplementation("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
    testImplementation("io.micronaut:micronaut-http-server-netty:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-runtime:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-inject:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-http-client:${micronautVersion}")
    testAnnotationProcessor("io.micronaut:micronaut-inject-java:${micronautVersion}")
    testImplementation("io.micronaut.rxjava2:micronaut-rxjava2:1.1.0")
    testImplementation("io.micronaut.rxjava2:micronaut-rxjava2-http-server-netty:1.1.0")
    testImplementation("io.micronaut.rxjava2:micronaut-rxjava2-http-client:1.1.0")
}
