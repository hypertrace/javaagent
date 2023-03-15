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
        versions = "[2.2, 3.0)"
        assertInverse = true
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
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${versions["opentelemetry_java_agent"]}")
    testRuntimeOnly("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap:${versions["opentelemetry_java_agent"]}")
    muzzleBootstrap("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap:${versions["opentelemetry_java_agent"]}")
    compileOnly("javax.servlet:servlet-api:2.2")

//    testInstrumentation(project(":instrumentation:servlet:servlet-3.0:javaagent"))
//
//    testLibrary("org.eclipse.jetty:jetty-server:7.0.0.v20091005")
//    testLibrary("org.eclipse.jetty:jetty-servlet:7.0.0.v20091005")
//
//    latestDepTestLibrary("org.eclipse.jetty:jetty-server:7.+") // see servlet-3.0 module
//    latestDepTestLibrary("org.eclipse.jetty:jetty-servlet:7.+") // see servlet-3.0 module
}
