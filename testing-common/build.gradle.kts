plugins {
    `java-library`
    `java-test-fixtures`
    id("net.bytebuddy.byte-buddy")
}

val versions: Map<String, String> by extra

dependencies {
    testFixturesApi(project(":otel-extensions"))

    testFixturesCompileOnly("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testFixturesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testFixturesImplementation("org.junit-pioneer:junit-pioneer:1.0.0")
    testFixturesApi("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
    testFixturesApi("io.opentelemetry:opentelemetry-sdk:${versions["opentelemetry"]}")
    testFixturesCompileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:${versions["opentelemetry"]}-alpha")
    testFixturesApi("com.squareup.okhttp3:okhttp:4.9.0")
    testFixturesApi("com.squareup.okhttp3:logging-interceptor:4.9.0")
    testFixturesImplementation("io.opentelemetry:opentelemetry-exporter-logging:${versions["opentelemetry"]}")
    testFixturesImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${versions["opentelemetry_java_agent"]}")
    testFixturesImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${versions["opentelemetry_java_agent-tooling"]}") {
        constraints {
            implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling-java9:1.7.2-alpha") {
                attributes {
                    // this transitive dependency creates classes compatible with Java 9 and up, but is only referenced in safe ways for
                    // java 8 by the javaagent-tooling dependency
                    attribute(Attribute.of("org.gradle.jvm.version", Integer::class.java), 9 as Integer)
                }
            }
        }
    }
    testFixturesImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetry_java_agent"]}")
    testFixturesImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${versions["opentelemetry"]}")
    testFixturesImplementation("ch.qos.logback:logback-classic:1.4.6")
    testFixturesImplementation("org.slf4j:log4j-over-slf4j:${versions["slf4j"]}")
    testFixturesImplementation("org.slf4j:jcl-over-slf4j:${versions["slf4j"]}")
    testFixturesImplementation("org.slf4j:jul-to-slf4j:${versions["slf4j"]}")
    testFixturesImplementation("net.bytebuddy:byte-buddy:${versions["byte_buddy"]}")
    testFixturesImplementation("net.bytebuddy:byte-buddy-agent:${versions["byte_buddy"]}")
    testFixturesCompileOnly("com.google.auto.service:auto-service-annotations:1.0")
    testFixturesAnnotationProcessor("com.google.auto.service:auto-service:1.0")
    testFixturesImplementation("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
}
