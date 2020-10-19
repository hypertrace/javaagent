plugins {
    java
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.10"
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

dependencies {
    api(project(":blocking"))

    compileOnly("javax.servlet:servlet-api:2.3")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-auto-servlet-2.2:0.9.0-20201009.101215-80")
}
