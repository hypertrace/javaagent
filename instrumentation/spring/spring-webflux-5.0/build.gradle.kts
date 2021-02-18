plugins {
    `java-library`
}

val versions: Map<String, String> by extra

dependencies {
    testImplementation(project(":testing-common"))
    testImplementation(project(":instrumentation:netty:netty-4.1"))

    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")

    testImplementation("org.springframework:spring-webflux:5.0.0.RELEASE")
    testImplementation("io.projectreactor.ipc:reactor-netty:0.7.0.RELEASE")

    testImplementation("org.springframework.boot:spring-boot-starter-webflux:2.0.0.RELEASE")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.0.0.RELEASE")
    testImplementation("org.springframework.boot:spring-boot-starter-reactor-netty:2.0.0.RELEASE")
}
