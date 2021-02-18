plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "javax.servlet"
        module = "javax.servlet-api"
        versions = "[3.0.0,3.0.1]"
    }
    fail {
        group = "javax.servlet"
        module = "servlet-api"
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

val versions: Map<String, String> by extra

dependencies {
    api(project(":instrumentation:servlet:servlet-common"))

    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:${versions["opentelemetry_java_agent"]}")

    compileOnly("javax.servlet:javax.servlet-api:3.0.1")

    testImplementation(project(":testing-common")) {
        exclude(group ="org.eclipse.jetty", module= "jetty-server")
    }
    testImplementation("org.eclipse.jetty:jetty-server:8.1.22.v20160922")
    testImplementation("org.eclipse.jetty:jetty-servlet:8.1.22.v20160922")
}


tasks.withType<Test> {
    jvmArgs = mutableListOf("-Dotel.instrumentation.servlet.enabled=true")
}
