plugins {
    `java-library`
    id("com.github.johnrengelman.shadow")
}

// Depend on all libraries that are in the bootstrap classloader when running the agent. When
// running tests, we simulate this by adding the jar produced by this project to the bootstrap
// classpath.

val versions: Map<String, String> by extra

dependencies {
    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${versions["opentelemetry_instrumentation_bom_alpha"]}"))
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
    implementation(project(":javaagent-core"))
    implementation(project(":filter-api"))

    implementation("ch.qos.logback:logback-classic:1.4.6")
    implementation("org.slf4j:slf4j-api:${versions["slf4j"]}")
}

tasks {
    shadowJar {
        archiveFileName.set("testing-bootstrap.jar")

        // need to exclude these logback classes from the bootstrap jar, otherwise tomcat will find them
        // and try to load them from the bootstrap class loader, which will fail with NoClassDefFoundError
        // since their super classes are servlet classes which are not in the bootstrap class loader
        exclude("ch/qos/logback/classic/servlet/LogbackServletContainerInitializer.class")
        exclude("ch/qos/logback/classic/servlet/LogbackServletContextListener.class")
        exclude("META-INF/services/javax.servlet.ServletContainerInitializer")
    }
}
