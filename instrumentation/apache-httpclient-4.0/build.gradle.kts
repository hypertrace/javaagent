plugins {
    `java-library`
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.10"
    id("io.opentelemetry.instrumentation.auto-instrumentation")
    muzzle
}

muzzle {
    // TODO this does not fail
//    fail {
//        group = "commons-httpclient"
//        module = "commons-httpclient"
//        versions = "[,4.0)"
//        skipVersions.add("3.1-jenkins-1")
//    }
    pass {
        group = "org.apache.httpcomponents"
        module = "httpclient"
        versions = "[4.0,)"
        assertInverse = true
    }
    pass {
        // We want to support the dropwizard clients too.
        group = "io.dropwizard"
        module = "dropwizard-client"
        versions = "(,)"
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
    implementation("org.apache.httpcomponents:httpclient:4.0")
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpclient-4.0:0.10.1")

    testImplementation(project(":testing-common"))
}
