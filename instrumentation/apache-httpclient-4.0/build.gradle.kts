plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}
evaluationDependsOn(":javaagent-tooling")

muzzle {
    // TODO this check fails, but it passes in OTEL https://github.com/hypertrace/javaagent/issues/144
//    fail {
//        group = "commons-httpclient"
//        module = "commons-httpclient"
//        versions = "[,4.0)"
//        skipVersions.add("3.1-jenkins-1")
//    }
    pass {
        group = "org.apache.httpcomponents"
        module = "httpclient"
        versions = "[4.0,)"
        assertInverse = true
    }
    pass {
        // We want to support the dropwizard clients too.
        group = "io.dropwizard"
        module = "dropwizard-client"
        versions = "(,3.0.0)"
        assertInverse = false
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
    api(project(":instrumentation:java-streams"))
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpclient-4.0:${versions["opentelemetry_java_agent"]}")
    library("org.apache.httpcomponents:httpclient:4.0")

     testImplementation(testFixtures(project(":testing-common")))
     testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:${versions["opentelemetry_api_semconv"]}")
}
