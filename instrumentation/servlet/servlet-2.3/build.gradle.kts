plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.10"
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
    byteBuddy {
        transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
            setTasks(setOf("compileJava", "compileScala", "compileKotlin"))
            plugin = "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin"
            setClassPath(project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath + sourceSets["main"].output)
        })
    }
}

dependencies {
    api(project(":instrumentation:servlet:servlet-common"))

    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-2.2:0.10.1")

    compileOnly("javax.servlet:servlet-api:2.3")

    testImplementation(project(":testing-common")){
        exclude(group ="org.eclipse.jetty", module= "jetty-server")
    }
    testImplementation("org.eclipse.jetty:jetty-server:7.5.4.v20111024")
    testImplementation("org.eclipse.jetty:jetty-servlet:7.5.4.v20111024")
}
