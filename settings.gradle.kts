rootProject.name = "hypertrace-agent"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://dl.bintray.com/hypertrace/maven")
    }
}

plugins {
    id("org.hypertrace.version-settings") version "0.1.6"
}

include(":javaagent")
include("instrumentation")
include("instrumentation:servlet:servlet-common")
findProject(":instrumentation:servlet:servlet-common")?.name = "servlet-common"
include("instrumentation:servlet:servlet-2.3")
findProject(":instrumentation:servlet:servlet-2.3")?.name = "servlet-2.3"
include("instrumentation:servlet:servlet-3.0")
findProject(":instrumentation:servlet:servlet-3.0")?.name = "servlet-3.0"
include("instrumentation:servlet:servlet-3.1")
findProject(":instrumentation:servlet:servlet-3.1")?.name = "servlet-3.1"
include("instrumentation:spark-2.3")
findProject(":instrumentation:spark-2.3")?.name = "spark-2.3"
include("instrumentation:apache-httpclient-4.0")
findProject(":instrumentation:apache-httpclient-4.0")?.name = "apache-httpclient-4.0"
include("smoke-tests")
include("filter-api")
include("javaagent-tooling")
include("javaagent-bootstrap")
include("javaagent-core")
include("testing-common")
include("instrumentation:grpc-1.5")
findProject(":instrumentation:grpc-1.5")?.name = "grpc-1.5"
include("instrumentation:okhttp:okhttp-3.0")
findProject(":instrumentation:okhttp:okhttp-3.0")?.name = "okhttp-3.0"
include("filter-custom-opa")
include("otel-extensions")
include("testing-bootstrap")
include("instrumentation:jaxrs-client-2.0")
findProject(":instrumentation:jaxrs-client-2.0")?.name = "jaxrs-client-2.0"
include("instrumentation:java-streams")
findProject(":instrumentation:java-streams")?.name = "java-streams"
