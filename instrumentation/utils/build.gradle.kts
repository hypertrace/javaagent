plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

val versions: Map<String, String> by extra

dependencies{
    implementation("javax.servlet:javax.servlet-api:3.1.0")
    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
}
