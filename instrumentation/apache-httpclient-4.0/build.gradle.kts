plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

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
        versions = "(,)"
        assertInverse = true
    }
}

afterEvaluate{
    io.opentelemetry.instrumentation.gradle.bytebuddy.ByteBuddyPluginConfigurator(project,
            sourceSets.main.get(),
            "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin",
            project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
    ).configure()
}

dependencies {
    implementation("org.apache.httpcomponents:httpclient:4.0")
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpclient-4.0:0.11.0")

    testImplementation(project(":testing-common"))
}
