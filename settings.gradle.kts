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
include("otel-extensions")
include("testing-bootstrap")
include("instrumentation:jaxrs-client-2.0")
findProject(":instrumentation:jaxrs-client-2.0")?.name = "jaxrs-client-2.0"
include("instrumentation:java-streams")
findProject(":instrumentation:java-streams")?.name = "java-streams"
include("instrumentation:apache-httpasyncclient-4.1")
findProject(":instrumentation:apache-httpasyncclient-4.1")?.name = "apache-httpasyncclient-4.1"
include("instrumentation:vertx-web-3.0")
findProject(":instrumentation:vertx-web-3.0")?.name = "vertx-web-3.0"
include("instrumentation:netty:netty-4.0")
findProject(":instrumentation:netty:netty-4.0")?.name = "netty-4.0"
include("instrumentation:netty:netty-4.1")
findProject(":instrumentation:netty:netty-4.1")?.name = "netty-4.1"
include("instrumentation:spring:spring-webflux-5.0")
findProject(":instrumentation:spring:spring-webflux-5.0")?.name = "spring-webflux-5.0"
include("instrumentation:micronaut-1.0")
findProject(":instrumentation:micronaut-1.0")?.name = "micronaut-1.0"
include("instrumentation:servlet:servlet-3.0-no-wrapping")
findProject(":instrumentation:servlet:servlet-3.0-no-wrapping")?.name = "servlet-3.0-no-wrapping"
include("instrumentation:servlet:servlet-rw")
findProject(":instrumentation:servlet:servlet-rw")?.name = "servlet-rw"
include("instrumentation:struts-2.3")
findProject(":instrumentation:struts-2.3")?.name = "struts-2.3"
