
plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}
evaluationDependsOn(":javaagent-tooling")

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
            io.opentelemetry.javaagent.tooling.muzzle.generation.MuzzleCodeGenerationPlugin::class.java.name,
        files(project(":javaagent-tooling").configurations["instrumentationMuzzle"], configurations.runtimeClasspath)
    ).configure()
}

val versions: Map<String, String> by extra

dependencies {
    api(project(":instrumentation:servlet:servlet-3.0"))

    muzzleBootstrap("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap:${versions["opentelemetry_java_agent"]}")

    compileOnly("com.sparkjava:spark-core:2.3")

    testImplementation(project(":testing-common"))
    testImplementation("com.sparkjava:spark-core:2.3")
}
