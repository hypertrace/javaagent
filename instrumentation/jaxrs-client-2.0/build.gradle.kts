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
            io.opentelemetry.javaagent.tooling.muzzle.generation.MuzzleCodeGenerationPlugin::class.java.name,
            project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
    ).configure()
}

val versions: Map<String, String> by extra

dependencies {
    api(project(":instrumentation:java-streams"))
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-http-url-connection:1.24.0-alpha")

    compileOnly("javax.ws.rs:javax.ws.rs-api:2.0.1")

    testImplementation(testFixtures(project(":testing-common")))
    testImplementation("org.glassfish.jersey.core:jersey-client:2.27")
    testImplementation("org.glassfish.jersey.inject:jersey-hk2:2.27")
    testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:${versions["opentelemetry_semconv"]}")

    testImplementation(project(":instrumentation:apache-httpclient-4.0"))
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpclient-4.0:${versions["opentelemetry_java_agent"]}")
    testImplementation("org.jboss.resteasy:resteasy-client:3.0.5.Final")
    // ^ This version has timeouts https://issues.redhat.com/browse/RESTEASY-975
    testImplementation("org.apache.cxf:cxf-rt-rs-client:3.1.0")
    // Doesn't work with CXF 3.0.x because their context is wrong:
    // https://github.com/apache/cxf/commit/335c7bad2436f08d6d54180212df5a52157c9f21

    testImplementation("javax.xml.bind:jaxb-api:2.2.3")

    testImplementation("org.glassfish.jersey.inject:jersey-hk2:2.+")
    testImplementation("org.glassfish.jersey.core:jersey-client:2.+")
    testImplementation("org.jboss.resteasy:resteasy-client:3.0.26.Final")
    testImplementation("org.apache.cxf:cxf-rt-rs-client:3.+")
}
