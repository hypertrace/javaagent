plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
}

val versions: Map<String, String> by extra

val micronautVersion = "1.0.0"
val micronautTestVersion = "1.0.0"

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
    testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:${versions["opentelemetry_java_agent"]}")
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")
}

val micronaut2Version = "2.2.3"

for (version in listOf(micronaut2Version)) {
    val versionedConfiguration = configurations.create("test_${version}") {
        extendsFrom(configurations.runtimeClasspath.get())
    }
    dependencies {
        versionedConfiguration(testFixtures(project(":testing-common")))
        versionedConfiguration("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
        versionedConfiguration("io.micronaut:micronaut-http-server-netty:${version}")
        versionedConfiguration("io.micronaut:micronaut-http-client:${version}")
        versionedConfiguration("io.micronaut:micronaut-runtime:${version}")
        versionedConfiguration("io.micronaut:micronaut-inject:${version}")
        versionedConfiguration("org.junit.jupiter:junit-jupiter-api:5.7.0")
        versionedConfiguration("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    }
    val versionedTest = task<Test>("test_${version}") {
        group = "verification"
        classpath = versionedConfiguration + sourceSets.test.get().output
        useJUnitPlatform()
    }
    tasks.check { dependsOn(versionedTest) }
}
