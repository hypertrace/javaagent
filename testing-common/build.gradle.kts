import com.google.protobuf.gradle.*

plugins {
    id("com.github.johnrengelman.shadow")
    `java-library`
    idea
    id("com.google.protobuf") version "0.9.4"
}

val protobufVersion = "3.19.6"

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    generateProtoTasks {
    }
}

idea {
    module {
        sourceDirs.add(file("${projectDir}/build/generated/source/proto/main/proto"))
    }
}

val versions: Map<String, String> by extra

dependencies {
    api(project(":otel-extensions"))

    compileOnly("org.junit.jupiter:junit-jupiter-api:5.7.0")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    implementation("org.junit-pioneer:junit-pioneer:1.0.0")
    api("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
    api("io.opentelemetry:opentelemetry-sdk:${versions["opentelemetry"]}")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:${versions["opentelemetry"]}")
    api("com.squareup.okhttp3:okhttp:4.9.0")
    api("com.squareup.okhttp3:logging-interceptor:4.9.0")
    implementation("io.opentelemetry:opentelemetry-exporter-logging:${versions["opentelemetry"]}")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${versions["opentelemetry"]}")
    implementation("ch.qos.logback:logback-classic:1.4.6")
    implementation("org.slf4j:log4j-over-slf4j:${versions["slf4j"]}")
    implementation("org.slf4j:jcl-over-slf4j:${versions["slf4j"]}")
    implementation("org.slf4j:jul-to-slf4j:${versions["slf4j"]}")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0")
    annotationProcessor("com.google.auto.service:auto-service:1.0")
    implementation("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
}

tasks {
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
        // relocate jetty so that tests using jetty do not conflict with this one
        relocate("org.eclipse.jetty", "io.opentelemetry.javaagent.shaded.org.eclipse.jetty")
    }
}
