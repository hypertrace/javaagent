plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.10"
}

dependencies {
    implementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.6.2")

    api("io.opentelemetry:opentelemetry-sdk:0.9.1")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.9.0")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:0.9.0")
    api("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.slf4j:log4j-over-slf4j:1.7.30")
    implementation("org.slf4j:jcl-over-slf4j:1.7.30")
    implementation("org.slf4j:jul-to-slf4j:1.7.30")
    implementation("net.bytebuddy:byte-buddy:1.10.10")
    implementation("net.bytebuddy:byte-buddy-agent:1.10.10")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
}
