plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    // TODO this check fails, but it passes in OTEL https://github.com/hypertrace/javaagent/issues/144
//    fail {
//        group.set("commons-httpclient")
//        module.set("commons-httpclient")
//        versions.set("[,4.0)")
//        skipVersions.add("3.1-jenkins-1")
//    }
    pass {
        group.set("org.apache.httpcomponents")
        module.set("httpclient")
        versions.set("[4.0,)")
        assertInverse.set(true)
    }
    pass {
        // We want to support the dropwizard clients too.
        group.set("io.dropwizard")
        module.set("dropwizard-client")
        versions.set("(,)")
        assertInverse.set(true)
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
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpclient-4.0:${versions["opentelemetry_java_agent"]}")

    library("org.apache.httpcomponents:httpclient:4.0")

     testImplementation(testFixtures(project(":testing-common")))
}
