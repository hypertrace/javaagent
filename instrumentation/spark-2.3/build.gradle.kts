
plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}
evaluationDependsOn(":javaagent-tooling")

// building against 2.3 and testing against 2.4 because JettyHandler is available since 2.4 only
muzzle {
    pass {
        group = "com.sparkjava"
        module = "spark-core"
        versions = "[2.3,)"
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
    api(project(":instrumentation:servlet:servlet-3.0"))

    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${versions["opentelemetry_instrumentation_bom_alpha"]}"))
    testRuntimeOnly("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap")
    muzzleBootstrap("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap")

    compileOnly("com.sparkjava:spark-core:2.3")

    testImplementation(project(":instrumentation:servlet:servlet-rw"))
    testImplementation(files(project(":instrumentation:servlet:servlet-rw").dependencyProject.sourceSets.main.map { it.output }))
    testImplementation(testFixtures(project(":testing-common")))
    testImplementation("com.sparkjava:spark-core:2.3")
    testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")
}
