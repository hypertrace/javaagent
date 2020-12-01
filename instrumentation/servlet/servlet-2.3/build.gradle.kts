plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy-gradle-plugin")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "javax.servlet"
        module = "servlet-api"
        versions = "[2.3,)"
    }
    fail {
        group = "javax.servlet"
        module = "javax.servlet-api"
        versions = "(,)"
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
    api(project(":instrumentation:servlet:servlet-common"))

    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-2.2:0.11.0")

    compileOnly("javax.servlet:servlet-api:2.3")

    testImplementation(project(":testing-common")){
        exclude(group ="org.eclipse.jetty", module= "jetty-server")
    }
    testImplementation("org.eclipse.jetty:jetty-server:7.5.4.v20111024")
    testImplementation("org.eclipse.jetty:jetty-servlet:7.5.4.v20111024")
}
