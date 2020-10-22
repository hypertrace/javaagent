plugins {
    `java-library`
}

dependencies{
    testImplementation("org.testcontainers:testcontainers:1.15.0-rc2")
    testImplementation("com.squareup.okhttp3:okhttp:4.9.0")
    testImplementation("org.awaitility:awaitility:4.0.3")
    testImplementation("io.opentelemetry:opentelemetry-proto:0.9.1")
    testImplementation("com.google.protobuf:protobuf-java-util:3.13.0")
}

tasks.test {
    useJUnitPlatform()
    reports {
        junitXml.isOutputPerTestCase = true
    }

    val shadowTask : Jar = project(":javaagent").tasks.named<Jar>("shadowJar").get()
    inputs.files(layout.files(shadowTask))

    doFirst {
        jvmArgs("-Dsmoketest.javaagent.path=${shadowTask.archiveFile.get()}")
    }
}
