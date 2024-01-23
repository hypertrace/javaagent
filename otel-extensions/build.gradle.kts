import com.google.protobuf.gradle.*

plugins {
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
    api(project(":filter-api"))
    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${versions["opentelemetry_instrumentation_bom_alpha"]}")) {
        constraints {
            implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling-java9") {
                attributes {
                    // this transitive dependency creates classes compatible with Java 9 and up, but is only referenced in safe ways for
                    // java 8 by the javaagent-tooling dependency
                    attribute(Attribute.of("org.gradle.jvm.version", Integer::class.java), 9 as Integer)
                }
            }
        }
    }

    compileOnly("io.opentelemetry:opentelemetry-sdk")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")

    implementation("io.opentelemetry.semconv:opentelemetry-semconv")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling") {
        exclude("io.opentelemetry.javaagent", "opentelemetry-javaagent-bootstrap")
    }

    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0")
    implementation("net.bytebuddy:byte-buddy-dep:${versions["byte_buddy"]}")
    annotationProcessor("com.google.auto.service:auto-service:1.0")


    api(platform("com.google.protobuf:protobuf-bom:$protobufVersion"))
    api("com.google.protobuf:protobuf-java")
    api("com.google.protobuf:protobuf-java-util")
    // convert yaml to json, since java protobuf impl supports only json
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0")
    // fix vulnerability
    constraints {
        api("com.google.code.gson:gson:2.8.9")
    }

    testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    testImplementation("io.opentelemetry:opentelemetry-sdk")
}
