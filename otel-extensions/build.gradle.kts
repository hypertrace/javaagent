plugins {
    `java-library`
}

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.30")
    compileOnly("io.opentelemetry:opentelemetry-sdk:0.10.0")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")

    testImplementation("io.opentelemetry:opentelemetry-sdk:0.10.0")
}
