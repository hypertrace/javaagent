plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group.set("io.grpc")
        module.set("grpc-netty-shaded")
        versions.set("[1.9.0,)")
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

dependencies {
    compileOnly("io.grpc:grpc-core:1.9.0")
    compileOnly("io.grpc:grpc-netty-shaded:1.9.0")
    implementation(project(":instrumentation:grpc-common"))
}