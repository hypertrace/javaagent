plugins {
    java
}

dependencies {
    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-auto-servlet-3.0:0.9.0-20201009.101216-80")
}

