plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}
evaluationDependsOn(":javaagent-tooling")

muzzle {
    pass {
        group = "org.apache.httpcomponents"
        module = "httpasyncclient"
        // 4.0 and 4.0.1 don't copy over the traceparent (etc) http headers on redirect
        versions = "[4.1,)"
        // TODO implement a muzzle check so that 4.0.x (at least 4.0 and 4.0.1) do not get applied
        //  and then bring back assertInverse
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
    api(project(":instrumentation:apache-httpclient-4.0"))

    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${versions["opentelemetry_instrumentation_bom_alpha"]}"))
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpasyncclient-4.1")
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpclient-4.0")
    testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")
    library("org.apache.httpcomponents:httpasyncclient:4.1")
     testImplementation(testFixtures(project(":testing-common")))
}

