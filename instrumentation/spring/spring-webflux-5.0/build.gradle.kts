plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
}

val versions: Map<String, String> by extra

dependencies {
    testImplementation(testFixtures(project(":testing-common")))
    testImplementation(project(":instrumentation:netty:netty-4.1"))
    testImplementation("org.springframework:spring-webflux:5.0.0.RELEASE")
    testImplementation("io.projectreactor.ipc:reactor-netty:0.7.0.RELEASE")

    testImplementation("org.springframework.boot:spring-boot-starter-webflux:2.0.0.RELEASE")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.0.0.RELEASE")
    testImplementation("org.springframework.boot:spring-boot-starter-reactor-netty:2.0.0.RELEASE")
}
