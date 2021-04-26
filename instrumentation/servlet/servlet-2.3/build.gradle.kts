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

val versions: Map<String, String> by extra

dependencies {
    api(project(":instrumentation:servlet:servlet-common"))

    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-2.2:${versions["opentelemetry_java_agent"]}")

    compileOnly("javax.servlet:servlet-api:2.3")

     testImplementation(testFixtures(project(":testing-common"))){
         if (this is ProjectDependency) {
             exclude(group ="org.eclipse.jetty", module= "jetty-server")
         } else {
             throw kotlin.IllegalStateException("could not exclude jetty server dependency")
         }
    }
    testImplementation("org.eclipse.jetty:jetty-server:7.5.4.v20111024")
    testImplementation("org.eclipse.jetty:jetty-servlet:7.5.4.v20111024")
}
