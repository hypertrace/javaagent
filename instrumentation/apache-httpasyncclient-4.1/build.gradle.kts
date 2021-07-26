plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group.set("org.apache.httpcomponents")
        module.set("httpasyncclient")
        // 4.0 and 4.0.1 don't copy over the traceparent (etc) http headers on redirect
        versions.set("[4.1,)")
        // TODO implement a muzzle check so that 4.0.x (at least 4.0 and 4.0.1) do not get applied
        //  and then bring back assertInverse
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
    api(project(":instrumentation:apache-httpclient-4.0"))

    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpasyncclient-4.1:${versions["opentelemetry_java_agent"]}")

    library("org.apache.httpcomponents:httpasyncclient:4.1")
     testImplementation(testFixtures(project(":testing-common")))
}

