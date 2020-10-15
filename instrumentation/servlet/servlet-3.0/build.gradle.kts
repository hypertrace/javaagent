plugins {
    java
}

muzzle {
    pass {
        group = "javax.servlet"
        module = "javax.servlet-api"
        versions = "[3.1.0,)"
//        assertInverse = true
    }
    fail {
        group = "javax.servlet"
        module = "servlet-api"
        versions = "(,3.0.1)"
    }
}

dependencies {
//    implementation(project(":blocking"))

    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    implementation("io.opentelemetry.instrumentation.auto:opentelemetry-auto-servlet-3.0:0.9.0-20201009.101216-80")
}
