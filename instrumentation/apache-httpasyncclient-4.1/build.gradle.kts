plugins {
    `java-library`
//    id("net.bytebuddy.byte-buddy")
//    id("io.opentelemetry.instrumentation.auto-instrumentation")
    id("com.github.johnrengelman.shadow")
//    muzzle
}

val testAgent by configurations.creating {
    isTransitive = false
}


val testInstrumentation by configurations.creating

configurations.compileOnly {
    extendsFrom(testInstrumentation)
}

//muzzle {
//    pass {
//        group = "org.apache.httpcomponents"
//        module = "httpasyncclient"
//        // 4.0 and 4.0.1 don't copy over the traceparent (etc) http headers on redirect
//        versions = "[4.1,)"
//        // TODO implement a muzzle check so that 4.0.x (at least 4.0 and 4.0.1) do not get applied
//        //  and then bring back assertInverse
//    }
//}

//afterEvaluate{
//    io.opentelemetry.instrumentation.gradle.bytebuddy.ByteBuddyPluginConfigurator(project,
//            sourceSets.main.get(),
//            "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin",
//            project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
//    ).configure()
//}

val versions: Map<String, String> by extra

dependencies {
    compileOnly(project(":instrumentation:java-streams"))
    testInstrumentation(project(":instrumentation:java-streams"))

    compileOnly(project(":instrumentation:apache-httpclient-4.0"))
    testInstrumentation(project(":instrumentation:apache-httpclient-4.0"))
    compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${versions["opentelemetry_java_agent"]}")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-instrumentation-api:${versions["opentelemetry_java_agent"]}")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetry_java_agent"]}")
    implementation("org.slf4j:slf4j-api:1.7.30")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0")
    annotationProcessor("com.google.auto.service:auto-service:1.0")

    testAgent("io.opentelemetry.javaagent", "opentelemetry-agent-for-testing", "${versions["opentelemetry_java_agent"]}", ext = "jar")

    compileOnly("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpasyncclient-4.1:${versions["opentelemetry_java_agent"]}")
    testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpasyncclient-4.1:${versions["opentelemetry_java_agent"]}")
    compileOnly("org.apache.httpcomponents:httpasyncclient:4.1")
    testImplementation("org.apache.httpcomponents:httpasyncclient:4.1")
    testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common:${versions["opentelemetry_java_agent"]}")

    testImplementation(testFixtures(project(":testing-common")))
}

tasks.shadowJar {
    configurations = listOf(project.configurations["runtimeClasspath"], testInstrumentation)
    mergeServiceFiles()
    archiveFileName.set("agent-testing.jar")
    // Prevents conflict with other SLF4J instances. Important for premain.
    relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")
    // rewrite dependencies calling Logger.getLogger
    relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

    // rewrite library instrumentation dependencies
    relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation")

    // relocate OpenTelemetry API usage
    relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
    relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
    relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")

    // relocate the OpenTelemetry extensions that are used by instrumentation modules
    // these extensions live in the AgentClassLoader, and are injected into the user's class loader
    // by the instrumentation modules that use them
    relocate("io.opentelemetry.extension.aws", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")
    relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")

}

tasks.withType<Test> {
    inputs.file(tasks.shadowJar.get().archiveFile.get())
    jvmArgs("-Dotel.javaagent.debug=true")
    jvmArgs("-Dotel.javaagent.experimental.initializer.jar=${tasks.shadowJar.get().archiveFile.get().asFile.absolutePath}")
    jvmArgs("-Dotel.javaagent.testing.additional-library-ignores.enabled=false")
    jvmArgs("-Dotel.javaagent.testing.fail-on-context-leak=true")
    // prevent sporadic gradle deadlocks, see SafeLogger for more details
    jvmArgs("-Dotel.javaagent.testing.transform-safe-logging.enabled=true")

    dependsOn(tasks.shadowJar)

    jvmArgs("-javaagent:${testAgent.files.first().absolutePath}")
}


