plugins {
    java
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

// afterEvaluate is needed for instrumenationMuzzle evaluation
afterEvaluate{
    byteBuddy {
        transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
            setTasks(setOf("compileJava", "compileScala", "compileKotlin"))
            plugin = "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin"
            setClassPath(instrumentationMuzzle + configurations.runtimeClasspath + sourceSets["main"].output)
        })
    }
}

val instrumentationMuzzle by configurations.creating

dependencies {
    api(project(":blocking"))

    compileOnly("javax.servlet:servlet-api:2.3")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-2.2:0.9.0")

    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:0.9.0")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:0.9.0")
    instrumentationMuzzle("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.9.0")
    instrumentationMuzzle("net.bytebuddy:byte-buddy:1.10.10")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-agent:1.10.10")
    instrumentationMuzzle("com.blogspot.mydailyjava:weak-lock-free:0.15")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0-rc7")
    instrumentationMuzzle("org.slf4j:slf4j-api:1.7.30")

    testImplementation("org.eclipse.jetty:jetty-server:7.6.21.v20160908")
    testImplementation("org.eclipse.jetty:jetty-servlet:7.6.21.v20160908")
    testImplementation("io.opentelemetry:opentelemetry-sdk:0.9.1")
    testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.9.0")
    testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:0.9.0")
    testImplementation("com.squareup.okhttp3:okhttp:4.9.0")
    testImplementation("cglib:cglib:3.2.5")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("org.slf4j:log4j-over-slf4j:1.7.30")
    testImplementation("org.slf4j:jcl-over-slf4j:1.7.30")
    testImplementation("org.slf4j:jul-to-slf4j:1.7.30")
    testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.9.0-20201013.195535-1")

}
