plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

// Depend on all libraries that are in the bootstrap classloader when running the agent. When
// running tests, we simulate this by adding the jar produced by this project to the bootstrap
// classpath.

dependencies {
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:0.11.0")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-api:0.11.0")
    implementation(project(":javaagent-core"))
    implementation(project(":filter-api"))
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
