import com.google.protobuf.gradle.*

plugins {
    `java-library`
    idea
    id("com.google.protobuf") version "0.8.13"
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.13.0"
    }
    generateProtoTasks {
    }
}

idea {
    module {
        sourceDirs.add(file("${projectDir}/build/generated/source/proto/main/proto"))
    }
}

dependencies {
    implementation("io.opentelemetry:opentelemetry-api:0.9.1") {
        exclude(group = "io.grpc", module = "grpc-context")
    }

    api("com.google.protobuf:protobuf-java:3.13.0"){
        exclude(group = "io.opentelemetry", module = "opentelemetry-api")
        exclude(group = "io.grpc", module = "grpc-context")
    }
    api("com.google.protobuf:protobuf-java-util:3.13.0") {
        exclude(group = "io.opentelemetry", module = "opentelemetry-api")
        exclude(group = "io.grpc", module = "grpc-context")
    }
//    api("io.grpc:grpc-netty:1.33.1")
    // convert yaml to json, since java protobuf impl supports only json
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.3")
}
