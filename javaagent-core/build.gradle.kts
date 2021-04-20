import com.google.protobuf.gradle.*

plugins {
    `java-library`
    idea
    id("com.google.protobuf") version "0.8.13"
    id("org.hypertrace.publish-maven-central-plugin")
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.11.4"
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

    api("com.google.protobuf:protobuf-java:3.11.4")
    api("com.google.protobuf:protobuf-java-util:3.11.4")
    // convert yaml to json, since java protobuf impl supports only json
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.3")

    api("com.blogspot.mydailyjava:weak-lock-free:0.17")
}
