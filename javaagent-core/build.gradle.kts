import com.google.protobuf.gradle.*

plugins {
    `java-library`
    idea
    id("com.google.protobuf") version "0.8.13"
    id("org.hypertrace.publish-maven-central-plugin")
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
    api("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-api:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-caching:${versions["opentelemetry_java_agent"]}")
    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")

    api(platform("com.google.protobuf:protobuf-bom:$protobufVersion"))
    api("com.google.protobuf:protobuf-java")
    api("com.google.protobuf:protobuf-java-util")
    // convert yaml to json, since java protobuf impl supports only json
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.3")

    implementation("com.blogspot.mydailyjava:weak-lock-free:0.17")
}
