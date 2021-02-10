plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
}

configurations {
    implementation.get().extendsFrom(project(":").configurations["testDependencies"])
}

val versions: Map<String, String> by extra

dependencies {
    api(project(":otel-extensions"))

    api("io.opentelemetry:opentelemetry-api:${versions["opentelemetry"]}")
    api("io.opentelemetry:opentelemetry-sdk:${versions["opentelemetry"]}")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:${versions["opentelemetry"]}-alpha")
    api("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-spi:${versions["opentelemetry_java_agent"]}")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${versions["opentelemetry_java_agent"]}")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.slf4j:log4j-over-slf4j:${versions["slf4j"]}")
    implementation("org.slf4j:jcl-over-slf4j:${versions["slf4j"]}")
    implementation("org.slf4j:jul-to-slf4j:${versions["slf4j"]}")
    implementation("net.bytebuddy:byte-buddy:${versions["byte_buddy"]}")
    implementation("net.bytebuddy:byte-buddy-agent:${versions["byte_buddy"]}")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    implementation("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
}
