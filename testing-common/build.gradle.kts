plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.10"
}

configurations {
    implementation.get().extendsFrom(project(":").configurations["testDependencies"])
}

dependencies {
    api("io.opentelemetry:opentelemetry-api:0.10.0")
    api("io.opentelemetry:opentelemetry-sdk:0.10.0")
    api("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:0.10.1")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:0.10.1")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.slf4j:log4j-over-slf4j:1.7.30")
    implementation("org.slf4j:jcl-over-slf4j:1.7.30")
    implementation("org.slf4j:jul-to-slf4j:1.7.30")
    implementation("net.bytebuddy:byte-buddy:1.10.10")
    implementation("net.bytebuddy:byte-buddy-agent:1.10.10")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    implementation("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
}
