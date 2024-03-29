plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}
evaluationDependsOn(":javaagent-tooling")

muzzle {
    pass {
        group = "javax.servlet"
        module = "javax.servlet-api"
        versions = "[3.0.0,)"
    }
    // fail on all old servlet-api. This groupId was changed in 3.x to javax.servlet-api
    fail {
        group = "javax.servlet"
        module = "servlet-api"
        versions = "(,)"
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
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:${versions["opentelemetry_java_agent"]}") // Servlet3Accessor
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${versions["opentelemetry_java_agent"]}")
    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    muzzleBootstrap("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap:${versions["opentelemetry_java_agent"]}")

    testImplementation(project(":testing-common") as ProjectDependency) {
        exclude(group = "org.eclipse.jetty", module = "jetty-server")
    }
    testImplementation("org.eclipse.jetty:jetty-server:8.1.22.v20160922")
    testImplementation("org.eclipse.jetty:jetty-servlet:8.1.22.v20160922")
}
