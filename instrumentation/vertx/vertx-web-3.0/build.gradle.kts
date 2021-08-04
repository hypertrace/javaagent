plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

afterEvaluate{
    io.opentelemetry.instrumentation.gradle.bytebuddy.ByteBuddyPluginConfigurator(project,
            sourceSets.main.get(),
            "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin",
            project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
    ).configure()
}

muzzle {
    pass {
        group = "io.vertx"
        module = "vertx-web"
        versions = "[3.0.0,4.0.0)"
//        extraDependency("io.netty:netty-codec-http:4.0.38.Final")
    }
}

val versions: Map<String, String> by extra
// version used by io.vertx:vertx-web:3.0.0
val nettyVersion = "4.0.28.Final"

dependencies {
    implementation(project(":instrumentation:vertx:otel-vertx-web-3.0-unshaded-for-instrumentation", "shadow"))
    library("io.vertx:vertx-web:3.0.0")

    testImplementation(testFixtures(project(":testing-common")))
    testImplementation(project(":instrumentation:netty:netty-4.0"))

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
}
