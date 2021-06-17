import com.google.protobuf.gradle.*

plugins {
    `java-library`
    idea
    id("com.google.protobuf") version "0.8.13"
}


val protobufVersion = "3.15.8"

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
    api(project(":filter-api"))

    compileOnly("io.opentelemetry:opentelemetry-sdk:${versions["opentelemetry"]}")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:${versions["opentelemetry"]}-alpha")
    implementation("io.opentelemetry:opentelemetry-semconv:${versions["opentelemetry"]}-alpha")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-spi:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${versions["opentelemetry_java_agent"]}")

    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0")
    implementation("net.bytebuddy:byte-buddy:${versions["byte_buddy"]}")
    annotationProcessor("com.google.auto.service:auto-service:1.0")


    api(platform("com.google.protobuf:protobuf-bom:$protobufVersion"))
    api("com.google.protobuf:protobuf-java")
    api("com.google.protobuf:protobuf-java-util")
    // convert yaml to json, since java protobuf impl supports only json
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.3")

    testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:${versions["opentelemetry"]}-alpha")
    testImplementation("io.opentelemetry:opentelemetry-sdk:${versions["opentelemetry"]}")
}
