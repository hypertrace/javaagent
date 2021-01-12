plugins {
    `java-library`
}

val versions: Map<String, String> by extra

val micronautVersion = "1.0.0"
val micronaut2Version = "2.2.3"
val micronautTestVersion = "1.0.0"

dependencies {
    implementation(project(":instrumentation:netty:netty-4.1"))
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")

    testImplementation(project(":testing-common"))
    testImplementation("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
    testImplementation("io.micronaut:micronaut-http-server-netty:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-runtime:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-inject:${micronautVersion}")
    testAnnotationProcessor("io.micronaut:micronaut-inject-java:${micronautVersion}")
}

/**
 * Todo this does not run tests
 */
for (version in listOf(micronautVersion, micronaut2Version)) {
    val versionedConfiguration = configurations.create("test_${version}") {
        extendsFrom(configurations.testRuntimeClasspath.get())
    }
    dependencies {
        versionedConfiguration(project(":instrumentation:netty:netty-4.1"))
        versionedConfiguration("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")
        versionedConfiguration(project(":testing-common"))
        versionedConfiguration("io.micronaut:micronaut-inject-java:${version}")
        versionedConfiguration("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
        versionedConfiguration("io.micronaut:micronaut-http-server-netty:${version}")
        versionedConfiguration("io.micronaut:micronaut-runtime:${version}")
        versionedConfiguration("io.micronaut:micronaut-inject:${version}")
    }
    val versionedTest = task<Test>("test_${version}") {
        group = "verification"
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = versionedConfiguration
        shouldRunAfter("test")
    }
    tasks.check { dependsOn(versionedTest) }
}
