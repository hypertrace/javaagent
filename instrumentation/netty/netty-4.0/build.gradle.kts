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
        versions.set("[4.0.0.Final,4.1.0.Final)")
        assertInverse.set(true)
    }
    pass {
        group.set("io.netty")
        module.set("netty-all")
        versions.set("[4.0.0.Final,4.1.0.Final)")
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
        versions.set("[2.0.0,3.3.0)")
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
// version used by async-http-client:2.0.9
val nettyVersion = "4.0.38.Final"

dependencies {
//    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.0:${versions["opentelemetry_java_agent"]}")
    implementation(project(":instrumentation:netty:otel-netty-4.0-unshaded-for-instrumentation", "shadow"))
    compileOnly("io.netty:netty-codec-http:${nettyVersion}") {
        version {
            strictly(nettyVersion)
        }
    }

    testImplementation("io.netty:netty-codec-http:${nettyVersion}") {
        version {
            strictly(nettyVersion)
        }
    }
    testImplementation("io.netty:netty-transport:${nettyVersion}") {
        version {
            strictly(nettyVersion)
        }
    }
    testImplementation("io.netty:netty-common:${nettyVersion}") {
        version {
            strictly(nettyVersion)
        }
    }
    testImplementation("io.netty:netty-codec:${nettyVersion}") {
        version {
            strictly(nettyVersion)
        }
    }
    testImplementation("io.netty:netty-handler:${nettyVersion}") {
        version {
            strictly(nettyVersion)
        }
    }
    testImplementation("io.netty:netty-buffer:${nettyVersion}") {
        version {
            strictly(nettyVersion)
        }
    }

     testImplementation(testFixtures(project(":testing-common")))
    testImplementation("org.asynchttpclient:async-http-client:2.0.9")
}

