plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "io.vertx"
        module = "vertx-web"
        versions = "[3.0.0,)"
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
    testImplementation(project(":testing-common"))
    testImplementation(project(":instrumentation:netty:netty-4.0"))
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.0:${versions["opentelemetry_java_agent"]}")
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-vertx-web-3.0:${versions["opentelemetry_java_agent"]}")
    testImplementation("io.vertx:vertx-web:3.0.0")
}

