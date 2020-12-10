plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "javax.ws.rs"
        module = "javax.ws.rs-api"
        versions = "[2.0,)"
    }
    pass {
        // We want to support the dropwizard clients too.
        group = "io.dropwizard"
        module = "dropwizard-client"
        versions = "[0.8.0,)"
        // TODO this is set in OTEL
//        assertInverse = true
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
    api(project(":instrumentation:java-streams"))
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-client-2.0-common:${versions["opentelemetry_java_agent"]}")

    compileOnly("javax.ws.rs:javax.ws.rs-api:2.0.1")

    testImplementation(project(":testing-common"))
    testImplementation("org.glassfish.jersey.core:jersey-client:2.27")
    testImplementation("org.glassfish.jersey.inject:jersey-hk2:2.27")
}
