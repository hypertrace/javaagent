plugins {
    `java-library`
    id("com.github.johnrengelman.shadow")
}

val versions: Map<String, String> by extra

repositories {
    mavenCentral()
}

dependencies {
    api(project(":filter-api"))
    compileOnly(project(":javaagent-bootstrap"))
    compileOnly("io.opentelemetry:opentelemetry-sdk")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetry_java_agent"]}")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${versions["opentelemetry_java_agent-tooling"]}")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0")
    annotationProcessor("com.google.auto.service:auto-service:1.0")
}

tasks{
    shadowJar{
        dependencies{
            // exclude packages that live in the bootstrap classloader
            exclude("io/opentelemetry/semconv/**")
            exclude("io/opentelemetry/context/**")
            exclude(dependency("io.opentelemetry:opentelemetry-api"))
            exclude(dependency("org.hypertrace.agent:filter-api"))
            exclude(dependency("org.hypertrace.agent:javaagent-core"))
            exclude("io/opentelemetry/instrumentation/api/**")
            // exclude bootstrap part of javaagent-extension-api
            exclude("io/opentelemetry/javaagent/bootstrap/**")
        }

        // relocate these in sync with
        // https://github.com/hypertrace/javaagent/blob/main/instrumentation/build.gradle.kts#L56-L82
        relocate("com.fasterxml.jackson", "org.hypertrace.io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.fasterxml.jackson")
        relocate("com.google", "org.hypertrace.io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.google")
        relocate("google.protobuf", "org.hypertrace.io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.google.protobuf")
        relocate("org.checkerframework", "org.hypertrace.io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.checkerframework") // transitive dependency form ht-filter-api
        relocate("org.yaml", "org.hypertrace.io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.org.yaml") // transitive dependency form ht-filter-api
        relocate("com.blogspot.mydailyjava.weaklockfree", "org.hypertrace.io.opentelemetry.instrumentation.api.internal.shaded.weaklockfree") // transitive dependency from ht-filter-api
        relocate("org.slf4j", "org.hypertrace.io.opentelemetry.javaagent.slf4j")
        relocate("java.util.logging.Logger", "org.hypertrace.io.opentelemetry.javaagent.bootstrap.PatchLogger")

        relocate("okhttp3", "org.hypertrace.javaagent.filter.com.squareup.okhttp3")
        relocate("okio", "org.hypertrace.javaagent.filter.com.squareup.okio") // transitive dependency from okhttp
        relocate("org.codehaus", "org.hypertrace.javaagent.filter.org.codehaus") // transitive dependency from ht-filter-api

        // relocate OpenTelemetry API in sync with
        // https://github.com/hypertrace/javaagent/blob/main/javaagent/build.gradle.kts#L58-L63
        relocate("io.opentelemetry.api", "org.hypertrace.io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
        relocate("io.opentelemetry.semconv", "org.hypertrace.io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
        relocate("io.opentelemetry.spi", "org.hypertrace.io.opentelemetry.javaagent.shaded.io.opentelemetry.spi")
        relocate("io.opentelemetry.context", "org.hypertrace.io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
        relocate("io.opentelemetry.extension.kotlin", "org.hypertrace.io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")
        relocate("io.opentelemetry.extension.aws", "org.hypertrace.io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")

        relocate("io.opentelemetry", "org.hypertrace.io.opentelemetry")

        manifest {
            attributes.put("Implementation-Title", "test-filter-impl")
            attributes.put("Implementation-Version", project.version)
            attributes.put("Implementation-Vendor", "Hypertrace")
        }
    }
}
