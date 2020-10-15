plugins {
    java
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.10"
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

subprojects {
    apply(plugin = "net.bytebuddy.byte-buddy-gradle-plugin")
    apply(plugin = "muzzle")
    apply(plugin = "io.opentelemetry.instrumentation.auto-instrumentation")
    dependencies {
        implementation("org.slf4j:slf4j-api:1.7.30")
        implementation("com.google.auto.service:auto-service:1.0-rc7")
        annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")

        implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-tooling:0.9.0-20201009.101126-80")
    }

    // set in gradle/instrumentation.gradle and applied to all instrumentations
    byteBuddy {
        transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
            setTasks(setOf("compileJava", "compileScala", "compileKotlin"))
            plugin = "io.opentelemetry.javaagent.tooling.muzzle.MuzzleGradlePlugin\$NoOp"
        })
    }
    afterEvaluate{
        byteBuddy {
            transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
                setTasks(setOf("compileJava", "compileScala", "compileKotlin"))
                plugin = "io.opentelemetry.javaagent.tooling.muzzle.MuzzleGradlePlugin"
                setClassPath(instrumentationMuzzle + project.configurations.runtimeClasspath + project.sourceSets["main"].output)
            })
        }
    }
}

val instrumentationMuzzle by configurations.creating

dependencies{
    implementation(project(":instrumentation:servlet:servlet-3.0"))

    instrumentationMuzzle("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-tooling:0.9.0-20201009.101126-80")
    instrumentationMuzzle("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-bootstrap:0.9.0-20201009.192532-82")
    instrumentationMuzzle("io.opentelemetry.instrumentation:opentelemetry-auto-api:0.9.0-20201009.192531-82")
    instrumentationMuzzle("net.bytebuddy:byte-buddy:1.10.10")
    instrumentationMuzzle("net.bytebuddy:byte-buddy-agent:1.10.10")
    instrumentationMuzzle("com.blogspot.mydailyjava:weak-lock-free:0.15")
    instrumentationMuzzle("com.google.auto.service:auto-service:1.0-rc7")
    instrumentationMuzzle("org.slf4j:slf4j-api:1.7.30")
//    instrumentationMuzzle(project(":blocking"))
}

tasks {
    // Keep in sync with https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/f893ca540b72a895fbf18c14d2df8d1cabaf2c7f/instrumentation/instrumentation.gradle#L51
    shadowJar {
        mergeServiceFiles()

        exclude("**/module-info.class")

        // Prevents conflict with other SLF4J instances. Important for premain.
        relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")
        // rewrite dependencies calling Logger.getLogger
        relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

        // prevents conflict with library instrumentation
        relocate("io.opentelemetry.instrumentation.api", "io.opentelemetry.javaagent.shaded.instrumentation.api")

        // relocate OpenTelemetry API
        relocate("io.opentelemetry.OpenTelemetry", "io.opentelemetry.javaagent.shaded.io.opentelemetry.OpenTelemetry")
        relocate("io.opentelemetry.common", "io.opentelemetry.javaagent.shaded.io.opentelemetry.common")
        relocate("io.opentelemetry.baggage", "io.opentelemetry.javaagent.shaded.io.opentelemetry.baggage")
        relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
        relocate("io.opentelemetry.internal", "io.opentelemetry.javaagent.shaded.io.opentelemetry.internal")
        relocate("io.opentelemetry.metrics", "io.opentelemetry.javaagent.shaded.io.opentelemetry.metrics")
        relocate("io.opentelemetry.trace", "io.opentelemetry.javaagent.shaded.io.opentelemetry.trace")

        //opentelemetry rewrite library instrumentation dependencies
        relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
            exclude("io.opentelemetry.instrumentation.auto.**")
        }

        // relocate OpenTelemetry API dependency
        relocate("io.grpc", "io.opentelemetry.javaagent.shaded.io.grpc")
    }
}
