plugins {
    java
}

dependencies {
    implementation(project(":blocking"))

    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-auto-servlet-3.0:0.9.0-SNAPSHOT")
    // This is published to local maven but under wrong groupId
//    implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-spi:0.9.0-SNAPSHOT")
    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-javaagent-spi:0.9.0-SNAPSHOT")
}

