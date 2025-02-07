import com.google.protobuf.gradle.id

plugins {
    `java-library`
    idea
    id("com.google.protobuf") version "0.9.4"
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}
evaluationDependsOn(":javaagent-tooling")

muzzle {
    pass {
        group = "io.grpc"
        module = "grpc-core"
        versions = "[1.6.0,)"

        extraDependency("com.google.protobuf:protobuf-java:3.3.1")
        extraDependency("io.grpc:grpc-netty:1.6.0")
    }
}

afterEvaluate{
    io.opentelemetry.instrumentation.gradle.bytebuddy.ByteBuddyPluginConfigurator(project,
            sourceSets.main.get(),
            io.opentelemetry.javaagent.tooling.muzzle.generation.MuzzleCodeGenerationPlugin::class.java.name,
    files(project(":javaagent-tooling").configurations["instrumentationMuzzle"], configurations.runtimeClasspath)
    ).configure()
}

idea {
    module {
        sourceDirs.add(file("${projectDir}/build/generated/source/proto/test/proto"))
    }
}

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
    all().configureEach {
      plugins {
        id("grpc")
      }
    }
  }
}

val versions: Map<String, String> by extra
val grpcVersion = "1.6.0"

dependencies {
    api("io.opentelemetry.instrumentation:opentelemetry-grpc-1.6:${versions["opentelemetry_java_agent"]}")
    implementation(project(":instrumentation:grpc-common"))
    implementation(project(":shaded-protobuf-java-util", "shadow"))

    compileOnly("io.grpc:grpc-core:${grpcVersion}")
    compileOnly("io.grpc:grpc-protobuf:${grpcVersion}") {
        exclude("com.google.protobuf", "protobuf-java-util")
    }
    compileOnly("io.grpc:grpc-stub:${grpcVersion}")
    compileOnly("io.grpc:grpc-netty:${grpcVersion}")

    implementation("javax.annotation:javax.annotation-api:1.3.2")

    testImplementation(project(":testing-common"))

    testImplementation(files(project(":instrumentation:grpc-shaded-netty-1.9").artifacts))

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

fun computeSourceSetNameForVersion(input: String): String {
  return "test_${input.replace(".","")}"
}

val grpcVersions = listOf(grpcVersion, "1.30.0")

sourceSets {
    for (version in grpcVersions) {
        create(computeSourceSetNameForVersion(version)) {
            dependencies {
                implementationConfigurationName("io.grpc:grpc-core:$version")
            }
        }
    }
}

tasks.compileTestJava {
    this.classpath += sourceSets.named(computeSourceSetNameForVersion(grpcVersion)).get().output
}
tasks.test {
    classpath += sourceSets.named(computeSourceSetNameForVersion(grpcVersion)).get().output
}

for (version in listOf("1.30.0")) {
    val versionedConfiguration = configurations.create("test_${version}") {
        extendsFrom(configurations.runtimeClasspath.get())
    }
    dependencies {
        versionedConfiguration(project(":testing-common"))
        versionedConfiguration(project(":instrumentation:grpc-shaded-netty-1.9"))
        versionedConfiguration(platform("io.grpc:grpc-bom:$version"))
        versionedConfiguration("io.grpc:grpc-core")
        versionedConfiguration("io.grpc:grpc-protobuf")
        versionedConfiguration("io.grpc:grpc-stub")
        versionedConfiguration("io.grpc:grpc-netty")
    }
    val versionedTest = task<Test>("test_${version}") {
        group = "verification"
        classpath = versionedConfiguration + sourceSets.main.get().output + sourceSets.test.get().output + sourceSets.named(computeSourceSetNameForVersion(version)).get().output
        // We do fine-grained filtering of the classpath of this codebase's sources since Gradle's
        // configurations will include transitive dependencies as well, which tests do often need.
        classpath = classpath.filter {
            if (file(layout.buildDirectory.dir("resources/main")).equals(it) || file(layout.buildDirectory.dir("classes/java/main")).equals(
                    it
                )
            ) {
                // The sources are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }

            val lib = it.absoluteFile
            if (lib.name.startsWith("opentelemetry-javaagent-")) {
                // These dependencies are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }
            if (lib.name.startsWith("javaagent-core")) {
                // These dependencies are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }
            if (lib.name.startsWith("filter-api")) {
                // These dependencies are packaged into the testing jar, so we need to exclude them from the test
                // classpath, which automatically inherits them, to ensure our shaded versions are used.
                return@filter false
            }
            if (lib.name.startsWith("opentelemetry-") && lib.name.contains("-autoconfigure-")) {
                // These dependencies should not be on the test classpath, because they will auto-instrument
                // the library and the tests could pass even if the javaagent instrumentation fails to apply
                return@filter false
            }
            return@filter true
        }
        useJUnitPlatform()
    }
    tasks.check { dependsOn(versionedTest) }
}
