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

val versions: Map<String, String> by extra

dependencies {
    compileOnly("com.squareup.okhttp3:okhttp:3.0.0")
    testImplementation(project(":instrumentation:otel-unshaded-for-testing:okhttp-unshaded", "shadow"))
    testImplementation(testFixtures(project(":testing-common")))
}
