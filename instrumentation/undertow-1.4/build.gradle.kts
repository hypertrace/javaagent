plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "io.undertow"
        module = "undertow-core"
        versions = "[1.4.0.Final,)"
        assertInverse = true
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
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-undertow-1.4:${versions["opentelemetry_java_agent"]}")
    library("io.undertow:undertow-core:2.0.0.Final")
    testImplementation(testFixtures(project(":testing-common")))
    testImplementation("javax.servlet:javax.servlet-api:3.1.0")
    testImplementation("io.undertow:undertow-servlet:2.0.0.Final")
    testRuntimeOnly(project(":instrumentation:servlet:servlet-3.0"))
    testRuntimeOnly(project(":instrumentation:undertow-servlet-1.4"))
}

