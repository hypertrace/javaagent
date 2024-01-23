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
    testFixturesApi(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${versions["opentelemetry_instrumentation_bom_alpha"]}"))
    testFixturesImplementation("org.junit-pioneer:junit-pioneer:1.0.0")
    testFixturesApi("io.opentelemetry:opentelemetry-api")
    testFixturesApi("io.opentelemetry:opentelemetry-sdk")
    testFixturesCompileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-alpha")
    testFixturesApi("com.squareup.okhttp3:okhttp:4.9.0")
    testFixturesApi("com.squareup.okhttp3:logging-interceptor:4.9.0")
    testFixturesImplementation("io.opentelemetry:opentelemetry-exporter-logging")
    testFixturesImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
    testFixturesImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
    testFixturesImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
    testFixturesImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
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
