plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}
evaluationDependsOn(":javaagent-tooling")

muzzle {
    pass {
        group = "jakarta.servlet"
        module = "jakarta.servlet-api"
        versions = "[5.0.0,)"
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
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-5.0:${versions["opentelemetry_java_agent"]}") // Servlet5Accessor
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${versions["opentelemetry_java_agent"]}")
    compileOnly("jakarta.servlet:jakarta.servlet-api:5.0.0")
    muzzleBootstrap("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap:${versions["opentelemetry_java_agent"]}")

    testImplementation(project(":testing-common", "shadow"))
    testCompileOnly("com.squareup.okhttp3:okhttp:4.9.0")
    testImplementation("org.eclipse.jetty:jetty-server:11.0.0")
    testImplementation("org.eclipse.jetty:jetty-servlet:11.0.0")
}
