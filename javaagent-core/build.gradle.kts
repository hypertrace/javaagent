import com.google.protobuf.gradle.*

plugins {
    `java-library`
    idea
    id("com.google.protobuf") version "0.8.13"
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

dependencies {
    api("io.opentelemetry:opentelemetry-api:0.10.0")
    implementation("org.slf4j:slf4j-api:1.7.30")

    api("com.google.protobuf:protobuf-java:3.11.4")
    api("com.google.protobuf:protobuf-java-util:3.11.4")
    // convert yaml to json, since java protobuf impl supports only json
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.3")
}
