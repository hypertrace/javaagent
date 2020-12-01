plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "com.squareup.okhttp3"
        module = "okhttp"
        versions = "[3.0,)"
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

dependencies {
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-okhttp-3.0:0.11.0")

    compileOnly("com.squareup.okhttp3:okhttp:3.0.0")

    testImplementation(project(":testing-common"))
}
