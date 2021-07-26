plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group.set("io.netty")
        module.set("netty-codec-http")
        versions.set("[4.1.0.Final,)")
        assertInverse.set(true)
    }
    pass {
        group.set("io.netty")
        module.set("netty-all")
        versions.set("[4.1.0.Final,)")
        assertInverse.set(true)
    }
    fail {
        group.set("io.netty")
        module.set("netty")
        versions.set("[,]")
    }
    pass {
        group.set("io.vertx")
        module.set("vertx-core")
        versions.set("[3.3.0,)")
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
    implementation("io.opentelemetry.instrumentation:opentelemetry-netty-4.1:${versions["opentelemetry_java_agent"]}")
    implementation(project(":instrumentation:netty:otel-netty-4.1-unshaded-for-instrumentation", "shadow"))
    library("io.netty:netty-codec-http:4.1.0.Final")

     testImplementation(testFixtures(project(":testing-common")))
    testImplementation("io.netty:netty-handler:4.1.0.Final")
    testImplementation("org.asynchttpclient:async-http-client:2.1.0")
}

