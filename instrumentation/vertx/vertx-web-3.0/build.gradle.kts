plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}
evaluationDependsOn(":javaagent-tooling")

afterEvaluate{
    io.opentelemetry.instrumentation.gradle.bytebuddy.ByteBuddyPluginConfigurator(project,
            sourceSets.main.get(),
            io.opentelemetry.javaagent.tooling.muzzle.generation.MuzzleCodeGenerationPlugin::class.java.name,
            files(project(":javaagent-tooling").configurations["instrumentationMuzzle"], configurations.runtimeClasspath)
    ).configure()
}

muzzle {
    pass {
        group = "io.vertx"
        module = "vertx-web"
        versions = "[3.0.0,4.0.0)"
    }
}

val versions: Map<String, String> by extra
// version used by io.vertx:vertx-web:3.0.0
val nettyVersion = "4.0.28.Final"

dependencies {
    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${versions["opentelemetry_instrumentation_bom_alpha"]}"))
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-vertx-web-3.0")
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-vertx-http-client-3.0")
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-vertx-http-client-common")
    library("io.vertx:vertx-web:3.0.0")

    testImplementation(testFixtures(project(":testing-common")))
    testImplementation(project(":instrumentation:netty:netty-4.0"))
    testImplementation(files(project(":instrumentation:netty:netty-4.0").dependencyProject.sourceSets.main.map { it.output }))

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

