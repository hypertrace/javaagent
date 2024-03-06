plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

evaluationDependsOn(":javaagent-tooling")

muzzle {
    pass {
        coreJdk()
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
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${versions["opentelemetry"]}")
    testImplementation(project(":testing-common"))
    testImplementation("io.opentelemetry.javaagent:opentelemetry-muzzle:${versions["opentelemetry_java_agent"]}")
}
