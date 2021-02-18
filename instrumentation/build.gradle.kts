plugins {
    id("com.github.johnrengelman.shadow") version "6.0.0"
    `java-library`
}

val versions: Map<String, String> by extra

subprojects {
    dependencies {
        implementation("org.slf4j:slf4j-api:1.7.30")
        implementation("com.google.auto.service:auto-service:1.0-rc7")
        annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")
        implementation("net.bytebuddy:byte-buddy:${versions["byte_buddy"]}")

        implementation("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
        implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${versions["opentelemetry_java_agent"]}")
        implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-api:${versions["opentelemetry_java_agent"]}")
        implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${versions["opentelemetry_java_agent"]}")
        implementation(project(":javaagent-core"))
        implementation(project(":filter-api"))
    }

    // This ensures to build jars for all dependencies in instrumentation module for ByteBuddy
    configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME) {
        attributes {
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named<LibraryElements>(LibraryElements.JAR))
        }
    }
}

dependencies{
    implementation(project(":instrumentation:servlet:servlet-common"))
    implementation(project(":instrumentation:servlet:servlet-2.3"))
    implementation(project(":instrumentation:servlet:servlet-3.0"))
    implementation(project(":instrumentation:servlet:servlet-3.1"))
    implementation(project(":instrumentation:servlet:servlet-rw"))
    implementation(project(":instrumentation:servlet:servlet-3.0-no-wrapping"))
    implementation(project(":instrumentation:spark-2.3"))
    implementation(project(":instrumentation:grpc-1.5"))
    implementation(project(":instrumentation:okhttp:okhttp-3.0"))
    implementation(project(":instrumentation:apache-httpclient-4.0"))
    implementation(project(":instrumentation:jaxrs-client-2.0"))
    implementation(project(":instrumentation:java-streams"))
    implementation(project(":instrumentation:apache-httpasyncclient-4.1"))
    implementation(project(":instrumentation:netty:netty-4.0"))
    implementation(project(":instrumentation:netty:netty-4.1"))
    implementation(project(":otel-extensions"))
}

tasks {
    // Keep in sync with https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/f893ca540b72a895fbf18c14d2df8d1cabaf2c7f/instrumentation/instrumentation.gradle#L51
    shadowJar {
        dependencies{
            // exclude core, it lives in the bootstrap classloader
            exclude(project(":javaagent-core"))
        }

        mergeServiceFiles()

        relocate("com.fasterxml.jackson", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.fasterxml.jackson")
        relocate("com.google", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.google")
        relocate("google.protobuf", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.google.protobuf")
        relocate("org.checkerframework", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.checkerframework")
        relocate("org.yaml", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.org.yaml")

        relocate("com.blogspot.mydailyjava", "io.opentelemetry.javaagent.shaded.org.hypertrace.shaded.com.blogspot.mydailyjava")

        exclude("**/module-info.class")

        relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")
        relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

//        // prevents conflict with library instrumentation
        relocate("io.opentelemetry.instrumentation.api", "io.opentelemetry.javaagent.shaded.instrumentation.api")
        //opentelemetry rewrite library instrumentation dependencies
        relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
            exclude("io.opentelemetry.javaagent.instrumentation.**")
        }

        // relocate OpenTelemetry API
        relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
        relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
        relocate("io.opentelemetry.spi", "io.opentelemetry.javaagent.shaded.io.opentelemetry.spi")
        relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
        relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")
        relocate ("io.opentelemetry.extension.trace.propagation", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.trace.propagation")
    }
}
