plugins {
    `java-library`
    id("com.google.osdetector") version "1.7.0"
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}
evaluationDependsOn(":javaagent-tooling")

muzzle {
    pass {
        group = "io.netty"
        module = "netty-codec-http"
        versions = "[4.0.0.Final,4.1.0.Final)"
        assertInverse = true
    }
    pass {
        group = "io.netty"
        module = "netty-all"
        versions = "[4.0.0.Final,4.1.0.Final)"
        assertInverse = true
    }
    fail {
        group = "io.netty"
        module = "netty"
        versions = "[,]"
    }
    pass {
        group = "io.vertx"
        module = "vertx-core"
        versions = "[2.0.0,3.3.0)"
        assertInverse = true
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
// version used by async-http-client:2.0.9
val nettyVersion = "4.0.38.Final"

dependencies {
    api(project(":otel-extensions"))
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.0:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4-common:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.instrumentation:opentelemetry-netty-4-common:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:${versions["opentelemetry_semconv"]}")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:${versions["opentelemetry_api_semconv"]}")
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

    testImplementation(project(":testing-common"))
    testImplementation("org.asynchttpclient:async-http-client:2.0.9")
}

