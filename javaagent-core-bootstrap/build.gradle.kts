import com.google.protobuf.gradle.*

// This artifact is located in the bootstrap classloader
// hence it should not use/expose any 3rd party libraries that might collide with user application
// classpath.

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

    // this is relocated
    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")

    compileOnly("com.google.auto.service:auto-service-annotations:1.0")
    annotationProcessor("com.google.auto.service:auto-service:1.0")
}
