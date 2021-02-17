plugins {
    groovy
    `java-library`
}

apply {
    from("$rootDir/gradle/java.gradle")
}

val versions: Map<String, String> by extra

dependencies{
    testImplementation(project(":testing-common"))
    testImplementation(project(":javaagent-core"))
    testImplementation("org.testcontainers:testcontainers:1.15.0")
    testImplementation("com.squareup.okhttp3:okhttp:4.9.0")
    testImplementation("org.awaitility:awaitility:4.0.3")
    testImplementation("io.opentelemetry:opentelemetry-proto:${versions["opentelemetry"]}")
    testImplementation("io.opentelemetry:opentelemetry-sdk:${versions["opentelemetry"]}")
    testImplementation("com.google.protobuf:protobuf-java-util:3.13.0")
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
    testImplementation("info.solidsoft.spock:spock-global-unroll:0.5.1")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    testImplementation("org.codehaus.groovy:groovy-all:2.5.11")
}

tasks.test {
    useJUnitPlatform()
    reports {
        junitXml.isOutputPerTestCase = true
    }

    maxParallelForks = 4
    val shadowTask : Jar = project(":javaagent").tasks.named<Jar>("shadowJar").get()
    inputs.files(layout.files(shadowTask))

    doFirst {
        jvmArgs("-Dsmoketest.javaagent.path=${shadowTask.archiveFile.get()}")
    }
}