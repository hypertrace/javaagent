plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group.set("com.squareup.okhttp3")
        module.set("okhttp")
        versions.set("[3.0,)")
        assertInverse.set(true)
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
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-okhttp-3.0:${versions["opentelemetry_java_agent"]}")

    compileOnly("com.squareup.okhttp3:okhttp:3.0.0")

     testImplementation(testFixtures(project(":testing-common")))
}
