plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.10"
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

// afterEvaluate is needed for instrumenationMuzzle evaluation
afterEvaluate{
    byteBuddy {
        transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
            setTasks(kotlin.collections.setOf("compileJava", "compileScala", "compileKotlin"))
            plugin = "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin"
            setClassPath(project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath + sourceSets["main"].output)
        })
    }
}

dependencies {
    api(project(":instrumentation:servlet:servlet-common"))

    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:0.9.0")

    compileOnly("javax.servlet:javax.servlet-api:3.0.1")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    testImplementation(project(":testing-common"))
    testImplementation("org.eclipse.jetty:jetty-server:8.1.22.v20160922")
    testImplementation("org.eclipse.jetty:jetty-servlet:8.1.22.v20160922")
}

