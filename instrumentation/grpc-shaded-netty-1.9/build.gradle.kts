plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy")
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "io.grpc"
        module = "grpc-netty-shaded"
        versions = "[1.9.0,)"
        assertInverse = true
    }
}

dependencies {
    compileOnly("io.grpc:grpc-core:1.9.0")
    compileOnly("io.grpc:grpc-netty-shaded:1.9.0")
    implementation(project(":instrumentation:grpc-common"))
}