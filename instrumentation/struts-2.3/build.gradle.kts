plugins {
    `java-library`
    id("io.opentelemetry.instrumentation.auto-instrumentation")
}

repositories {
    mavenCentral()
}
val versions: Map<String, String> by extra
dependencies {
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:${versions["opentelemetry_java_agent"]}")
    testImplementation("junit:junit:4.12")
    testImplementation("org.apache.struts:struts2-core:2.3.1")
    testImplementation("org.apache.struts:struts2-json-plugin:2.3.1")
    testImplementation("org.apache.struts:struts2-convention-plugin:2.3.1")
    testImplementation("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
    testImplementation("org.eclipse.jetty:jetty-servlet:8.0.0.v20110901")
    testImplementation("javax.servlet:javax.servlet-api:3.0.1")
    testImplementation("javax.servlet:jsp-api:2.0")
    testImplementation(project(":testing-common")) {
        exclude(group ="org.eclipse.jetty", module= "jetty-server")
    }
    testImplementation(project(":instrumentation:servlet:servlet-rw"))
    testImplementation(project(":instrumentation:servlet:servlet-3.0-no-wrapping"))
}
