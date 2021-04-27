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
        versions = "[3.1.0,)"
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
            "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin",
            project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
    ).configure()
}

val versions: Map<String, String> by extra

dependencies {
    api(project(":instrumentation:servlet:servlet-common"))

    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:${versions["opentelemetry_java_agent"]}")

    compileOnly("javax.servlet:javax.servlet-api:3.1.0")

    testImplementation(testFixtures(project(":testing-common")) as ProjectDependency) {
        exclude(group = "org.eclipse.jetty", module = "jetty-server")
    }
    testImplementation("org.eclipse.jetty:jetty-server:9.4.32.v20200930")
    testImplementation("org.eclipse.jetty:jetty-servlet:9.4.32.v20200930")
}

tasks.withType<Test> {
    jvmArgs = mutableListOf("-Dotel.instrumentation.servlet.enabled=true")
}
