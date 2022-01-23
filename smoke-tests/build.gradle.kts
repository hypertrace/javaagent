plugins {
    groovy
    `java-library`
}

apply {
    from("$rootDir/gradle/java.gradle")
}

val versions: Map<String, String> by extra

dependencies{
    testImplementation(testFixtures(project(":testing-common")))
    testImplementation(project(":javaagent-core"))
    testImplementation("org.testcontainers:testcontainers:1.15.2")
    testImplementation("com.squareup.okhttp3:okhttp:4.9.0")
    testImplementation("org.awaitility:awaitility:4.0.3")
    testImplementation("io.opentelemetry.proto:opentelemetry-proto:${versions["opentelemetry_proto"]}")
    testImplementation("io.grpc:grpc-core:1.36.1") // needed at runtime to send gRPC requests to the gRPC app
    testRuntimeOnly("io.grpc:grpc-netty-shaded:1.36.1") // needed at runtime to send gRPC requests to the gRPC app
    testRuntimeOnly("io.grpc:grpc-stub:1.36.1") // needed at runtime to send gRPC requests to the gRPC app
    testRuntimeOnly("io.grpc:grpc-protobuf:1.36.1") // needed at runtime to send gRPC requests to the gRPC app
    testImplementation("io.opentelemetry:opentelemetry-sdk:${versions["opentelemetry"]}")
    testImplementation("com.google.protobuf:protobuf-java-util:3.15.8")
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
    testImplementation("info.solidsoft.spock:spock-global-unroll:0.5.1")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    testImplementation("org.codehaus.groovy:groovy-all:2.5.11")
    testImplementation("io.opentelemetry:opentelemetry-semconv:${versions["opentelemetry"]}-alpha")
}

tasks.test {
    useJUnitPlatform()
    reports {
        junitXml.isOutputPerTestCase = true
    }

    maxParallelForks = 2

    var suites : HashMap<String, String>
            = HashMap()
    suites.put("glassfish", "**/GlassFishSmokeTest.*")
    suites.put("jetty", "**/JettySmokeTest.*")
    suites.put("liberty", "**/LibertySmokeTest.*")
    suites.put("tomcat", "**/TomcatSmokeTest.*")
    suites.put("tomee" , "**/TomeeSmokeTest.*")
    suites.put("wildfly", "**/WildflySmokeTest.*")

    val suite = findProperty("smokeTestSuite")

    if (suite != null) {
        if ("other" == suite) {
            for ((key, value) in suites) {
                exclude(value)
            }
        } else if (suites.containsKey(suite)) {
            include(suites.get(suite))
        } else {
            throw GradleException("Unknown smoke test suite: " + suite)
        }
    }

    val shadowTask : Jar = project(":javaagent").tasks.named<Jar>("shadowJar").get()
    inputs.files(layout.files(shadowTask))

    doFirst {
        jvmArgs("-Dsmoketest.javaagent.path=${shadowTask.archiveFile.get()}")
    }
}
