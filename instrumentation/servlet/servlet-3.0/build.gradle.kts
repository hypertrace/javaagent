plugins {
    java
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
            setClassPath(instrumentationMuzzle + configurations.runtimeClasspath + sourceSets["main"].output)
        })
    }
}

val instrumentationMuzzle by configurations.creating

dependencies {
    api(project(":blocking"))

    compileOnly("javax.servlet:javax.servlet-api:3.0.1")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:0.9.0")

    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:0.9.0")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:0.9.0")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.9.0")
    instrumentationMuzzle("net.bytebuddy:byte-buddy:1.10.10")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-agent:1.10.10")
    instrumentationMuzzle("com.blogspot.mydailyjava:weak-lock-free:0.15")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0-rc7")
    instrumentationMuzzle("org.slf4j:slf4j-api:1.7.30")
}

