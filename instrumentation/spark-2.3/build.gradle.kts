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
            io.opentelemetry.javaagent.muzzle.generation.MuzzleCodeGenerationPlugin::class.java.name,
            project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
    ).configure()
}

val versions: Map<String, String> by extra

dependencies {
    api(project(":instrumentation:servlet:servlet-3.0"))

    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-spark-2.3:${versions["opentelemetry_java_agent"]}")
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:${versions["opentelemetry_java_agent"]}")
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jetty-8.0:${versions["opentelemetry_java_agent"]}")

    compileOnly("com.sparkjava:spark-core:2.3")

    testImplementation(project(":instrumentation:servlet:servlet-rw"))
    testImplementation(testFixtures(project(":testing-common")))
    testImplementation("com.sparkjava:spark-core:2.3")
}
