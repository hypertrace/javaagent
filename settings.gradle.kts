rootProject.name = "hypertrace-agent"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://hypertrace.jfrog.io/artifactory/maven")
    }
}

plugins {
    id("org.hypertrace.version-settings") version "0.2.0"
}

include(":javaagent")
include("instrumentation")
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
include("instrumentation:grpc-1.6")
findProject(":instrumentation:grpc-1.6")?.name = "grpc-1.6"
include("instrumentation:grpc-common")
findProject(":instrumentation:grpc-common")?.name = "grpc-common"
include("instrumentation:grpc-shaded-netty-1.9")
findProject(":instrumentation:grpc-shaded-netty-1.9")?.name = "grpc-shaded-netty-1.9"
include("shaded-protobuf-java-util")
findProject(":shaded-protobuf-java-util")?.name = "shaded-protobuf-java-util"
include("instrumentation:okhttp:okhttp-3.0")
findProject(":instrumentation:okhttp:okhttp-3.0")?.name = "okhttp-3.0"
include("otel-extensions")
include("testing-bootstrap")
include("instrumentation:jaxrs-client-2.0")
findProject(":instrumentation:jaxrs-client-2.0")?.name = "jaxrs-client-2.0"
include("instrumentation:java-streams")
findProject(":instrumentation:java-streams")?.name = "java-streams"
include("instrumentation:apache-httpasyncclient-4.1")
findProject(":instrumentation:apache-httpasyncclient-4.1")?.name = "apache-httpasyncclient-4.1"
include("instrumentation:vertx:vertx-web-3.0")
findProject(":instrumentation:vertx:vertx-web-3.0")?.name = "vertx-web-3.0"
include("instrumentation:netty:netty-4.0")
include("instrumentation:netty:otel-netty-4.0-unshaded-for-instrumentation")
include("instrumentation:netty:otel-netty-4.1-unshaded-for-instrumentation")
findProject(":instrumentation:netty:netty-4.0")?.name = "netty-4.0"
include("instrumentation:netty:netty-4.1")
findProject(":instrumentation:netty:netty-4.1")?.name = "netty-4.1"
include("instrumentation:spring:spring-webflux-5.0")
findProject(":instrumentation:spring:spring-webflux-5.0")?.name = "spring-webflux-5.0"
include("instrumentation:micronaut-1.0")
findProject(":instrumentation:micronaut-1.0")?.name = "micronaut-1.0"
include("instrumentation:servlet:servlet-3.0")
findProject(":instrumentation:servlet:servlet-3.0")?.name = "servlet-3.0"
include("instrumentation:servlet:servlet-rw")
findProject(":instrumentation:servlet:servlet-rw")?.name = "servlet-rw"
include("instrumentation:struts-2.3")
findProject(":instrumentation:struts-2.3")?.name = "struts-2.3"
include("instrumentation:undertow:otel-undertow-1.4-unshaded-for-instrumentation")
findProject(":instrumentation:undertow:otel-undertow-1.4-unshaded-for-instrumentation")?.name = "otel-undertow-1.4-unshaded-for-instrumentation"
include("instrumentation:undertow:undertow-common")
findProject(":instrumentation:undertow:undertow-common")?.name = "undertow-common"
include("instrumentation:undertow:undertow-1.4")
findProject(":instrumentation:undertow:undertow-1.4")?.name = "undertow-1.4"
include("instrumentation:undertow:undertow-servlet-1.4")
findProject(":instrumentation:undertow:undertow-servlet-1.4")?.name = "undertow-servlet-1.4"
include("instrumentation:vertx:otel-vertx-web-3.0-unshaded-for-instrumentation")
findProject(":instrumentation:vertx:otel-vertx-web-3.0-unshaded-for-instrumentation")?.name = "otel-vertx-web-3.0-unshaded-for-instrumentation"
include("instrumentation:otel-unshaded-for-testing:spark-unshaded")
findProject(":instrumentation:otel-unshaded-for-testing:spark-unshaded")?.name = "spark-unshaded"
include("instrumentation:otel-unshaded-for-testing:servlet-unshaded")
findProject(":instrumentation:otel-unshaded-for-testing:servlet-unshaded")?.name = "servlet-unshaded"
include("instrumentation:otel-unshaded-for-testing:jetty-unshaded")
findProject(":instrumentation:otel-unshaded-for-testing:jetty-unshaded")?.name = "jetty-unshaded"
include("instrumentation:otel-unshaded-for-testing:okhttp-unshaded")
findProject(":instrumentation:otel-unshaded-for-testing:okhttp-unshaded")?.name = "okhttp-unshaded"
