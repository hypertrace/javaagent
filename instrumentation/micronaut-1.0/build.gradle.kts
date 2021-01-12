plugins {
    `java-library`
}

val versions: Map<String, String> by extra

val micronautVersion = "1.0.0"
val micronaut2Version = "2.2.3"
val micronautTestVersion = "2.3.1"

//val micronautLatest by configurations.getting {
//    extendsFrom(configurations.implementation.get())
//}

dependencies {
    implementation(project(":instrumentation:netty:netty-4.1"))
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")

    testImplementation(project(":testing-common"))

    testImplementation("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
    testImplementation("io.micronaut:micronaut-http-server-netty:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-runtime:${micronautVersion}")
    testImplementation("io.micronaut:micronaut-inject:${micronautVersion}")

    testAnnotationProcessor("io.micronaut:micronaut-inject-java:${micronautVersion}")

//    micronautLatest(":testing-common")
//    micronautLatest("io.micronaut.test:micronaut-test-junit5:${micronautTestVersion}")
//    micronautLatest("io.micronaut:micronaut-http-server-netty:${micronaut2Version}")
//    micronautLatest("io.micronaut:micronaut-runtime:${micronaut2Version}")
//    micronautLatest("io.micronaut:micronaut-inject:${micronaut2Version}")
}

//val micronaut2Test = task<Test>("micronaut2Test") {
//    description = "Runs micronaut2 tests."
//    group = "verification"
//
//    testClassesDirs = sourceSets["intTest"].output.classesDirs
//    classpath = sourceSets["intTest"].runtimeClasspath
//    shouldRunAfter("test")
//}
//
//tasks.check { dependsOn(micronaut2Test) }

for (version in listOf("1.0.0", "2.2.3")) {
    val versionedConfiguration = configurations.create("test_${version}") {
        extendsFrom(configurations.testRuntimeClasspath.get())
    }
    dependencies {
        versionedConfiguration(project(":instrumentation:netty:netty-4.1"))
        versionedConfiguration("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")
        versionedConfiguration(project(":testing-common"))
        versionedConfiguration("io.micronaut:micronaut-inject-java:${version}")
        versionedConfiguration("io.micronaut.test:micronaut-test-junit5:${version}")
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
