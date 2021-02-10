plugins {
    `java-library`
}

val versions: Map<String, String> by extra

dependencies {
    api(project(":filter-custom-opa"))

    compileOnly("io.opentelemetry:opentelemetry-sdk:${versions["opentelemetry"]}")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:${versions["opentelemetry"]}-alpha")
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-spi:${versions["opentelemetry_java_agent"]}")

    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    implementation("net.bytebuddy:byte-buddy:${versions["byte_buddy"]}")
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")

    testImplementation("io.opentelemetry:opentelemetry-sdk:${versions["opentelemetry"]}")
}
