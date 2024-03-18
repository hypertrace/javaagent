plugins {
    `java-library`
    id("io.opentelemetry.instrumentation.auto-instrumentation")
}

val versions: Map<String, String> by extra

dependencies {
    testImplementation(project(":testing-common") as ProjectDependency) {
        exclude(group = "org.eclipse.jetty", module = "jetty-server")
    }
    testImplementation("org.apache.struts:struts2-core:2.3.1")
    testImplementation("org.apache.struts:struts2-json-plugin:2.3.1")
    testImplementation("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
    testImplementation("org.eclipse.jetty:jetty-servlet:8.0.0.v20110901")
    testImplementation("javax.servlet:javax.servlet-api:3.0.1")
    testImplementation("javax.servlet:jsp-api:2.0")
}
