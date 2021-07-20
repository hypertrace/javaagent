import com.google.protobuf.gradle.*

plugins {
    `java-library`
    idea
    id("com.google.protobuf") version "0.8.13"
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "io.grpc"
        module = "grpc-core"
        versions = "[1.6.0,)"
        // for body capture via com.google.protobuf.util.JsonFormat
        extraDependency("io.grpc:grpc-protobuf:1.6.0")
        extraDependency("io.grpc:grpc-netty:1.6.0")
    }
}

afterEvaluate{
    io.opentelemetry.instrumentation.gradle.bytebuddy.ByteBuddyPluginConfigurator(project,
            sourceSets.main.get(),
            "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin",
    project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath
    ).configure()
}

idea {
    module {
        sourceDirs.add(file("${projectDir}/build/generated/source/proto/test/proto"))
    }
}

val testGrpcVersion = "1.30.0"

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.3.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.6.0"
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

val versions: Map<String, String> by extra
val grpcVersion = "1.6.0"

dependencies {
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-grpc-1.6:${versions["opentelemetry_java_agent"]}")
    api("io.opentelemetry.instrumentation:opentelemetry-grpc-1.6:${versions["opentelemetry_java_agent"]}")

    compileOnly("io.grpc:grpc-core:${grpcVersion}")
    compileOnly("io.grpc:grpc-protobuf:${grpcVersion}")
    compileOnly("io.grpc:grpc-stub:${grpcVersion}")
    compileOnly("io.grpc:grpc-netty:${grpcVersion}")

    implementation("javax.annotation:javax.annotation-api:1.3.2")

     testImplementation(testFixtures(project(":testing-common")))
    testImplementation("io.grpc:grpc-core:${grpcVersion}") {
        version {
            strictly(grpcVersion)
        }
    }
    testImplementation("io.grpc:grpc-protobuf:${grpcVersion}") {
        version {
            strictly(grpcVersion)
        }
    }
    testImplementation("io.grpc:grpc-stub:${grpcVersion}") {
        version {
            strictly(grpcVersion)
        }
    }
    testImplementation("io.grpc:grpc-netty:${grpcVersion}") {
        version {
            strictly(grpcVersion)
        }
    }
}

val grpcVersions = listOf(grpcVersion, "1.30.0")

sourceSets {
    for (version in grpcVersions) {
        create("test_$version") {
            dependencies {
                implementationConfigurationName("io.grpc:grpc-core:$version")
            }
        }
    }
}

tasks.compileTestJava {
    this.classpath += sourceSets.named("test_$grpcVersion").get().output
}
tasks.test {
    classpath += sourceSets.named("test_$grpcVersion").get().output
}

for (version in listOf("1.30.0")) {
    val versionedConfiguration = configurations.create("test_${version}") {
        extendsFrom(configurations.runtimeClasspath.get())
    }
    dependencies {
        versionedConfiguration(testFixtures(project(":testing-common")))
        versionedConfiguration("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-grpc-1.6:${versions["opentelemetry_java_agent"]}")
        versionedConfiguration("io.opentelemetry.instrumentation:opentelemetry-grpc-1.6:${versions["opentelemetry_java_agent"]}")
        versionedConfiguration(platform("io.grpc:grpc-bom:$version"))
        versionedConfiguration("io.grpc:grpc-core")
        versionedConfiguration("io.grpc:grpc-protobuf")
        versionedConfiguration("io.grpc:grpc-stub")
        versionedConfiguration("io.grpc:grpc-netty")

    }
    val versionedTest = task<Test>("test_${version}") {
        group = "verification"
        classpath = versionedConfiguration + sourceSets.test.get().output + sourceSets.named("test_$version").get().output
        useJUnitPlatform()
    }
    tasks.check { dependsOn(versionedTest) }
}
