rootProject.name = "hypertrace-agent"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://dl.bintray.com/hypertrace/maven")
    }
}

plugins {
    id("org.hypertrace.version-settings") version "0.1.5"
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
include("instrumentation:spark-web-framework-2.3")
findProject(":instrumentation:spark-web-framework-2.3")?.name = "spark-web-framework-2.3"
include("smoke-tests")
include("blocking")
include("javaagent-tooling")
include("javaagent-bootstrap")
include("javaagent-core")
include("testing-common")
