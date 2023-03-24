plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "javax.servlet"
        module = "servlet-api"
        versions = "[2.2,3.0)"
    }
    fail {
        group = "javax.servlet"
        module = "javax.servlet-api"
        versions = "(,)"
    }
}

afterEvaluate{
    io.opentelemetry.instrumentation.gradle.bytebuddy.ByteBuddyPluginConfigurator(
        project,
        sourceSets.main.get(),
        io.opentelemetry.javaagent.tooling.muzzle.generation.MuzzleCodeGenerationPlugin::class.java.name,
        project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
    ).configure()
}

val versions: Map<String, String> by extra

dependencies {
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-2.2:${versions["opentelemetry_java_agent"]}") // Servlet2Accessor
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-2.2:${versions["opentelemetry_java_agent"]}")
    testImplementation("io.opentelemetry.javaagent:opentelemetry-muzzle:${versions["opentelemetry_java_agent"]}")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${versions["opentelemetry_java_agent"]}")
    compileOnly("javax.servlet:servlet-api:2.2")
    testRuntimeOnly("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap:${versions["opentelemetry_java_agent"]}")
    muzzleBootstrap("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap:${versions["opentelemetry_java_agent"]}")

    testImplementation(project(":instrumentation:servlet:servlet-rw"))
    testImplementation(testFixtures(project(":testing-common")) as ProjectDependency) {
        exclude(group = "org.eclipse.jetty", module = "jetty-server")
    }
    testImplementation("org.eclipse.jetty:jetty-server:7.0.0.v20091005")
    testImplementation("org.eclipse.jetty:jetty-servlet:7.0.0.v20091005")
    testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:${versions["opentelemetry_semconv"]}")
}
