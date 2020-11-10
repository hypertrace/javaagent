plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.10"
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    pass {
        group = "com.squareup.okhttp3"
        module = "okhttp"
        versions = "[3.0,)"
        assertInverse = true
    }
}

afterEvaluate{
    byteBuddy {
        transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
            setTasks(kotlin.collections.setOf("compileJava", "compileScala", "compileKotlin"))
            plugin = "io.opentelemetry.javaagent.tooling.muzzle.collector.MuzzleCodeGenerationPlugin"
            setClassPath(project(":javaagent-tooling").configurations["instrumentationMuzzle"] + configurations.runtimeClasspath + sourceSets["main"].output)
        })
    }
}

dependencies {
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-okhttp-3.0:0.9.0")

    implementation("com.squareup.okhttp3:okhttp:3.0.0")
    implementation("net.bytebuddy:byte-buddy:1.10.10")

    testImplementation(project(":testing-common"))
    testImplementation("com.squareup.okhttp3:mockwebserver:3.0.0")
}
