plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
}

val versions: Map<String, String> by extra

configurations.testRuntimeClasspath {
    // We have newer version of slf4j but spring5 uses older versions
    resolutionStrategy {
        force("ch.qos.logback:logback-classic:1.2.11")
        force("org.slf4j:slf4j-api:1.7.36")
    }
}

dependencies {
    testImplementation(project(":testing-common"))
    testImplementation("org.springframework:spring-webflux:5.0.0.RELEASE")
    testImplementation("io.projectreactor.ipc:reactor-netty:0.7.0.RELEASE")

    testImplementation("org.springframework.boot:spring-boot-starter-webflux:2.0.0.RELEASE")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.0.0.RELEASE")
    testImplementation("org.springframework.boot:spring-boot-starter-reactor-netty:2.0.0.RELEASE")
}
