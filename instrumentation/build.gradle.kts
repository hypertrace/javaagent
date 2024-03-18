plugins {
    id("com.github.johnrengelman.shadow")
    `java-library`
}

dependencies{
    implementation(project(":instrumentation:servlet:servlet-rw"))
    implementation(project(":instrumentation:servlet:servlet-3.0"))
    implementation(project(":instrumentation:spark-2.3"))
    implementation(project(":instrumentation:grpc-1.6"))
    implementation(project(":instrumentation:grpc-shaded-netty-1.9"))
    implementation(project(":instrumentation:okhttp:okhttp-3.0"))
    implementation(project(":instrumentation:apache-httpclient-4.0"))
    implementation(project(":instrumentation:jaxrs-client-2.0"))
    implementation(project(":instrumentation:java-streams"))
    implementation(project(":instrumentation:apache-httpasyncclient-4.1"))
    implementation(project(":instrumentation:netty:netty-4.0"))
    implementation(project(":instrumentation:netty:netty-4.1"))
    implementation(project(":instrumentation:undertow:undertow-1.4"))
    implementation(project(":instrumentation:undertow:undertow-servlet-1.4"))
    implementation(project(":instrumentation:vertx:vertx-web-3.0"))
    implementation(project(":otel-extensions"))
}

tasks {
    // Keep in sync with https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/f893ca540b72a895fbf18c14d2df8d1cabaf2c7f/instrumentation/instrumentation.gradle#L51
    shadowJar {
        dependencies{
            // exclude packages that live in the bootstrap classloader
            exclude(project(":javaagent-core"))
            exclude(project(":filter-api"))
            exclude("io/opentelemetry/semconv/**")
            exclude("io/opentelemetry/context/**")
            exclude(dependency("io.opentelemetry:opentelemetry-api"))
            exclude("io/opentelemetry/instrumentation/api/**")
            // exclude bootstrap part of javaagent-extension-api
            exclude("io/opentelemetry/javaagent/bootstrap/**")
        }

        mergeServiceFiles()

        relocate("com.blogspot.mydailyjava.weaklockfree", "io.opentelemetry.instrumentation.api.internal.shaded.weaklockfree")

        exclude("**/module-info.class")

        relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")
        relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

//        // prevents conflict with library instrumentation
        relocate("io.opentelemetry.instrumentation.api", "io.opentelemetry.javaagent.shaded.instrumentation.api")
        //opentelemetry rewrite library instrumentation dependencies
        relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
            exclude("io.opentelemetry.javaagent.instrumentation.**")
        }

        // relocate OpenTelemetry API
        relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
        relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
        relocate("io.opentelemetry.spi", "io.opentelemetry.javaagent.shaded.io.opentelemetry.spi")
        relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
        relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")
        relocate("io.opentelemetry.extension.aws", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")
    }
}

subprojects {
    class JavaagentTestArgumentsProvider(
        @InputFile
        @PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
        val agentShadowJar: File,

        @InputFile
        @PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
        val shadowJar: File,

        @InputFile
        @PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
        val extensionJar: File,
    ) : CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> = listOf(
            "-Dotel.javaagent.debug=true",
            "-javaagent:${agentShadowJar.absolutePath}",
            "-Dht.javaagent.filter.jar.paths=${extensionJar.absolutePath}",
            "-Dotel.exporter.otlp.protocol=http/protobuf",
            "-Dotel.exporter.otlp.traces.endpoint=http://localhost:4318/v1/traces",
            "-Dotel.metrics.exporter=none",
            // make the path to the javaagent available to tests
            "-Dotel.javaagent.testing.javaagent-jar-path=${agentShadowJar.absolutePath}",
            "-Dotel.javaagent.experimental.initializer.jar=${shadowJar.absolutePath}",

            // prevent sporadic gradle deadlocks, see SafeLogger for more details
            "-Dotel.javaagent.testing.transform-safe-logging.enabled=true",
            // Reduce noise in assertion messages since we don't need to verify this in most tests. We check
            // in smoke tests instead.
            "-Dotel.javaagent.add-thread-details=false",
            "-Dotel.javaagent.experimental.indy=${findProperty("testIndy") == "true"}",
            // suppress repeated logging of "No metric data to export - skipping export."
            // since PeriodicMetricReader is configured with a short interval
            "-Dio.opentelemetry.javaagent.slf4j.simpleLogger.log.io.opentelemetry.sdk.metrics.export.PeriodicMetricReader=INFO",
            // suppress a couple of verbose ClassNotFoundException stack traces logged at debug level
            "-Dio.opentelemetry.javaagent.slf4j.simpleLogger.log.io.grpc.internal.ServerImplBuilder=INFO",
            "-Dio.opentelemetry.javaagent.slf4j.simpleLogger.log.io.grpc.internal.ManagedChannelImplBuilder=INFO",
            "-Dio.opentelemetry.javaagent.slf4j.simpleLogger.log.io.perfmark.PerfMark=INFO",
            "-Dio.opentelemetry.javaagent.slf4j.simpleLogger.log.io.grpc.Context=INFO"
        )
    }

    tasks.withType<Test>().configureEach {
        val instShadowTask: Jar = project(":instrumentation").tasks.named<Jar>("shadowJar").get()
        inputs.files(layout.files(instShadowTask))
        val instShadowJar = instShadowTask.archiveFile.get().asFile

        val shadowTask: Jar = project(":javaagent").tasks.named<Jar>("shadowJar").get()
        inputs.files(layout.files(shadowTask))
        val agentShadowJar = shadowTask.archiveFile.get().asFile

        val extensionBuild: Jar = project(":tests-extension").tasks.named<Jar>("shadowJar").get()
        inputs.files(layout.files(extensionBuild))
        val extensionJar = extensionBuild.archiveFile.get().asFile

        dependsOn(":instrumentation:shadowJar")

        dependsOn(":javaagent:shadowJar")

        dependsOn(":tests-extension:shadowJar")

        jvmArgumentProviders.add(JavaagentTestArgumentsProvider(agentShadowJar, instShadowJar, extensionJar))

        // We do fine-grained filtering of the classpath of this codebase's sources since Gradle's
        // configurations will include transitive dependencies as well, which tests do often need.
        classpath = classpath.filter {
            if (file(layout.buildDirectory.dir("resources/main")).equals(it) || file(layout.buildDirectory.dir("classes/java/main")).equals(
                    it
                )
            ) {
                // The sources are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }

            val lib = it.absoluteFile
            if (lib.name.startsWith("opentelemetry-javaagent-")) {
                // These dependencies are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }
            if (lib.name.startsWith("javaagent-core")) {
                // These dependencies are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }
            if (lib.name.startsWith("filter-api")) {
                // These dependencies are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }
            if (lib.name.startsWith("opentelemetry-") && lib.name.contains("-autoconfigure-")) {
                // These dependencies should not be on the test classpath, because they will auto-instrument
                // the library and the tests could pass even if the javaagent instrumentation fails to apply
                return@filter false
            }
            return@filter true
        }
    }

    configurations.configureEach {
        if (name.endsWith("testruntimeclasspath", ignoreCase = true)) {
            // Added by agent, don't let Gradle bring it in when running tests.
            exclude("io.opentelemetry.javaagent", "opentelemetry-javaagent-bootstrap")
        }
    }

}
