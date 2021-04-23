plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

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
            "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin",
            project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
    ).configure()
}

val versions: Map<String, String> by extra

val library by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = false
}

library.dependencies.whenObjectAdded {
    val dep = this.copy()
    configurations.testImplementation.get().dependencies.add(dep)
}
configurations.compileOnly.get().extendsFrom(library)

dependencies {
    api(project(":instrumentation:java-streams"))
    api(project(":instrumentation:apache-httpclient-4.0"))

    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpasyncclient-4.1:${versions["opentelemetry_java_agent"]}")

    library("org.apache.httpcomponents:httpasyncclient:4.1")
    testImplementation(project(":testing-common"))
}

