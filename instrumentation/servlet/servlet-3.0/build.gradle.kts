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
        versions = "[3.1.0,)"
    }
    fail {
        group = "javax.servlet"
        module = "servlet-api"
        versions = "(,3.0.1)"
    }
}

// afterEvaluate is needed for instrumenationMuzzle evaluation
afterEvaluate{
    byteBuddy {
        transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
            setTasks(setOf("compileJava", "compileScala", "compileKotlin"))
            plugin = "io.opentelemetry.javaagent.tooling.muzzle.MuzzleGradlePlugin"
            setClassPath(instrumentationMuzzle + configurations.runtimeClasspath + sourceSets["main"].output)
        })
    }
}

val instrumentationMuzzle by configurations.creating

dependencies {
    implementation(project(":blocking"))

    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-auto-servlet-3.0:0.9.0-20201009.101216-80")

    instrumentationMuzzle("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-tooling:0.9.0-20201009.101126-80")
    instrumentationMuzzle("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-bootstrap:0.9.0-20201009.192532-82")
    instrumentationMuzzle("io.opentelemetry.instrumentation:opentelemetry-auto-api:0.9.0-20201009.192531-82")
    instrumentationMuzzle("net.bytebuddy:byte-buddy:1.10.10")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-agent:1.10.10")
    instrumentationMuzzle("com.blogspot.mydailyjava:weak-lock-free:0.15")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0-rc7")
    instrumentationMuzzle("org.slf4j:slf4j-api:1.7.30")
}
