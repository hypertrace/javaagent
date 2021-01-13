plugins {
    `java-library`
}

val versions: Map<String, String> by extra

val micronautVersion = "1.0.0"
val micronautTestVersion = "1.0.0"

dependencies {
    implementation(project(":instrumentation:netty:netty-4.1"))
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")

    testImplementation(project(":testing-common"))
    testImplementation("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
    testImplementation("io.micronaut:micronaut-http-server-netty:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-runtime:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-inject:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-http-client:${micronautVersion}")
    testAnnotationProcessor("io.micronaut:micronaut-inject-java:${micronautVersion}")
}

val micronaut2Version = "2.2.3"

for (version in listOf(micronaut2Version)) {
    val versionedConfiguration = configurations.create("test_${version}") {
        extendsFrom(configurations.testRuntimeClasspath.get())
    }
    dependencies {
        versionedConfiguration(project(":instrumentation:netty:netty-4.1"))
        versionedConfiguration("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")
        versionedConfiguration(project(":testing-common"))
        versionedConfiguration("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
        versionedConfiguration("io.micronaut:micronaut-inject-java:${version}")
        versionedConfiguration("io.micronaut:micronaut-http-server-netty:${version}")
        versionedConfiguration("io.micronaut:micronaut-http-client:${version}")
        versionedConfiguration("io.micronaut:micronaut-runtime:${version}")
        versionedConfiguration("io.micronaut:micronaut-inject:${version}")
        versionedConfiguration("org.junit.jupiter:junit-jupiter-api:5.7.0")
        versionedConfiguration("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    }
    val versionedTest = task<Test>("test_${version}") {
        group = "verification"
        val testOuput = sourceSets.test.get().output
        testClassesDirs = testOuput.classesDirs
        classpath = versionedConfiguration + testOuput
        useJUnitPlatform()
        shouldRunAfter("test")
    }
    tasks.test { dependsOn(versionedTest) }
}
