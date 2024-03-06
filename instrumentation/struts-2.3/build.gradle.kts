plugins {
    `java-library`
    id("io.opentelemetry.instrumentation.auto-instrumentation")
}

val versions: Map<String, String> by extra

dependencies {
    testImplementation(project(":instrumentation:servlet:servlet-rw"))
    testImplementation(files(project(":instrumentation:servlet:servlet-rw").dependencyProject.sourceSets.main.map { it.output }))
    testImplementation(project(":instrumentation:servlet:servlet-3.0"))
    testImplementation(files(project(":instrumentation:servlet:servlet-3.0").dependencyProject.sourceSets.main.map { it.output }))
    testImplementation(project(":testing-common") as ProjectDependency) {
        exclude(group = "org.eclipse.jetty", module = "jetty-server")
    }
    testImplementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:${versions["opentelemetry_java_agent"]}")
    testRuntimeOnly("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap:${versions["opentelemetry_java_agent"]}")
    testImplementation("org.apache.struts:struts2-core:2.3.1")
    testImplementation("org.apache.struts:struts2-json-plugin:2.3.1")
    testImplementation("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
    testImplementation("org.eclipse.jetty:jetty-servlet:8.0.0.v20110901")
    testImplementation("javax.servlet:javax.servlet-api:3.0.1")
    testImplementation("javax.servlet:jsp-api:2.0")
    testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:${versions["opentelemetry_api_semconv"]}")
}
