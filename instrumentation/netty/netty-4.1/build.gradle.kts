plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "io.netty"
        module = "netty-codec-http"
        versions = "[4.1.0.Final,)"
        assertInverse = true
    }
    pass {
        group = "io.netty"
        module = "netty-all"
        versions = "[4.1.0.Final,)"
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
        versions = "[3.3.0,)"
        assertInverse = true
    }
}

afterEvaluate{
    io.opentelemetry.instrumentation.gradle.bytebuddy.ByteBuddyPluginConfigurator(project,
            sourceSets.main.get(),
            io.opentelemetry.javaagent.tooling.muzzle.generation.MuzzleCodeGenerationPlugin::class.java.name,
            project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
    ).configure()
}

val versions: Map<String, String> by extra

dependencies {
    implementation("io.opentelemetry.instrumentation:opentelemetry-netty-4.1:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4-common:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${versions["opentelemetry_java_agent"]}")
    library("io.netty:netty-codec-http:4.1.0.Final")

     testImplementation(testFixtures(project(":testing-common")))
    testImplementation("io.netty:netty-handler:4.1.0.Final")
    testImplementation("org.asynchttpclient:async-http-client:2.1.0")
}

