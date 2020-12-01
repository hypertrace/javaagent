plugins {
    `java-library`
}

dependencies {
    compileOnly("io.opentelemetry:opentelemetry-sdk:0.11.0")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-spi:0.11.0")

    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")

    implementation("net.bytebuddy:byte-buddy:1.10.18")
    testImplementation("io.opentelemetry:opentelemetry-sdk:0.11.0")
}
