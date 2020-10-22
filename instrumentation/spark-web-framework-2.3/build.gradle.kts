plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.10"
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

// building against 2.3 and testing against 2.4 because JettyHandler is available since 2.4 only
muzzle {
    pass {
        group = "com.sparkjava"
        module = "spark-core"
        versions = "[2.3,)"
    }
}

afterEvaluate{
    byteBuddy {
        transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
            setTasks(kotlin.collections.setOf("compileJava", "compileScala", "compileKotlin"))
            plugin = "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin"
            setClassPath(instrumentationMuzzle + configurations.runtimeClasspath + sourceSets["main"].output + project(":instrumentation:servlet:servlet-3.1").sourceSets["main"].output)
        })
    }
}

val instrumentationMuzzle by configurations.creating

dependencies {
    api(project(":blocking"))
    api(project(":instrumentation:servlet:servlet-3.1"))

    compileOnly("com.sparkjava:spark-core:2.3")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-spark-web-framework-2.3:0.9.0")
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:0.9.0")
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jetty-8.0:0.9.0")

    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:0.9.0")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:0.9.0")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.9.0")
    instrumentationMuzzle("net.bytebuddy:byte-buddy:1.10.10")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-agent:1.10.10")
    instrumentationMuzzle("com.blogspot.mydailyjava:weak-lock-free:0.15")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0-rc7")
    instrumentationMuzzle("org.slf4j:slf4j-api:1.7.30")

    testImplementation(project(":testing-common"))
    testImplementation("com.sparkjava:spark-core:2.3")
}
