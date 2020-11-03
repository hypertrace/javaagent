import com.google.protobuf.gradle.*

plugins {
    `java-library`
    idea
    id("com.google.protobuf") version "0.8.13"
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.10"
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "io.grpc"
        module = "grpc-core"
        versions = "[1.5.0, 1.33.0)" // see https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1453
        // for body capture via com.google.protobuf.util.JsonFormat
        extraDependency("io.grpc:grpc-protobuf:1.5.0")
    }
}

afterEvaluate{
    byteBuddy {
        transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
            setTasks(kotlin.collections.setOf("compileJava", "compileScala", "compileKotlin"))
            plugin = "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin"
            setClassPath(project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath + sourceSets["main"].output)
        })
    }
}

idea {
    module {
        sourceDirs.add(file("${projectDir}/build/generated/source/proto/main/proto"))
    }
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.3.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.5.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc") {
                }
            }
        }
    }
}

dependencies {
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-grpc-1.5:0.9.0")
    api("io.opentelemetry.instrumentation:opentelemetry-grpc-1.5:0.9.0")

    compileOnly("io.grpc:grpc-core:1.5.0")
    compileOnly("io.grpc:grpc-protobuf:1.5.0")
    compileOnly("io.grpc:grpc-stub:1.5.0")

    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    testImplementation(project(":testing-common"))
    testImplementation("io.grpc:grpc-core:1.5.0")
    testImplementation("io.grpc:grpc-protobuf:1.5.0")
    testImplementation("io.grpc:grpc-stub:1.5.0")
    testImplementation("io.grpc:grpc-netty:1.5.0")
}
