plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

// building against 2.3 and testing against 2.4 because JettyHandler is available since 2.4 only
muzzle {
    pass {
        group = "com.sparkjava"
        module = "spark-core"
        versions = "[2.3,)"
    }
}

afterEvaluate{
    io.opentelemetry.instrumentation.gradle.bytebuddy.ByteBuddyPluginConfigurator(project,
            sourceSets.main.get(),
            "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin",
            project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
    ).configure()
}

val versions: Map<String, String> by extra

dependencies {
    api(project(":instrumentation:servlet:servlet-3.0"))

    testImplementation(project(":instrumentation:otel-unshaded-for-testing:spark-unshaded", "shadow"))
    testImplementation(project(":instrumentation:otel-unshaded-for-testing:servlet-unshaded", "shadow"))
    testImplementation(project(":instrumentation:otel-unshaded-for-testing:jetty-unshaded", "shadow"))

    compileOnly("com.sparkjava:spark-core:2.3")

    testImplementation(project(":instrumentation:servlet:servlet-rw"))
    testImplementation(testFixtures(project(":testing-common")))
    testImplementation("com.sparkjava:spark-core:2.3")
}
